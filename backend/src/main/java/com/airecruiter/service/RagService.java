package com.airecruiter.service;

import com.airecruiter.dto.RecruiterDtos.*;
import com.airecruiter.model.AuditLog;
import com.airecruiter.model.Candidate;
import com.airecruiter.repository.AuditLogRepository;
import com.airecruiter.repository.CandidateRepository;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.Points.ScoredPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static io.qdrant.client.VectorsFactory.vectors;

/**
 * RAG (Retrieval-Augmented Generation) pipeline.
 *
 * Flow for every recruiter query:
 *   1. Detect bias in the query
 *   2. Embed the query → float[] via Ollama
 *   3. Search Qdrant for top-K similar resume chunks
 *   4. Build a prompt: system instructions + retrieved chunks + recruiter query
 *   5. Call Ollama (llama3.1:8b) to generate a structured answer
 *   6. Sanitize output (remove protected characteristic mentions)
 *   7. Log everything to audit table
 *   8. Return structured response
 *
 * The "AI" work is steps 2 and 5 — two HTTP calls.
 * Everything else is Java service orchestration — your territory.
 */
@Slf4j
@Service
public class RagService {

    private final EmbeddingService embeddingService;
    private final BiasCheckerService biasCheckerService;
    private final QdrantClient qdrantClient;
    private final ChatModel chatModel;                    // Spring AI: OllamaChatModel
    private final CandidateRepository candidateRepository;
    private final AuditLogRepository auditLogRepository;

    @Value("${qdrant.collections.resumes}")
    private String resumeCollection;

    @Value("${app.rag.top-k}")
    private int defaultTopK;

    @Value("${app.default-tenant-id}")
    private UUID defaultTenantId;

    @Value("${spring.ai.openai..chat.options.model}")
    private String chatModelName;


    public RagService(EmbeddingService embeddingService,
                      BiasCheckerService biasCheckerService,
                      QdrantClient qdrantClient,
                      @Qualifier("openAiChatModel") ChatModel chatModel,  // ← Groq
                      CandidateRepository candidateRepository,
                      AuditLogRepository auditLogRepository) {
        this.embeddingService = embeddingService;
        this.biasCheckerService = biasCheckerService;
        this.qdrantClient = qdrantClient;
        this.chatModel = chatModel;
        this.candidateRepository = candidateRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Main entry point: processes a recruiter's natural language query.
     */
    public RagQueryResponse query(RagQueryRequest request) {
        int topK = request.getTopK() > 0 ? request.getTopK() : defaultTopK;
        String query = request.getQuery();

        log.info("Processing RAG query: '{}'", query);

        // Step 1: Bias check
        boolean biasDetected = biasCheckerService.detectBiasInQuery(query);

        log.info("Processing detectBiasInQuery: '{}'", biasDetected);

        // Step 2: Embed the query
        log.info("Processing embedQuery:");
        float[] queryVector = embeddingService.embedQuery(query);



        // Step 3: Retrieve top-K chunks from Qdrant
        log.info("Processing searchQdrant:");
        List<ScoredPoint> hits = searchQdrant(queryVector, topK);

        log.info("Retrieve top-{} chunks from Qdrant:", hits.size());

        // Step 4: Extract text from results and look up candidate metadata
        log.info("Processing buildMatches:");
        List<CandidateMatch> matches = buildMatches(hits);

        log.info("Extract text from results and look up candidate metadata: {}", matches.size());

        // Step 5: Build prompt and generate answer
        log.info("Processing buildContext:");
        String context = buildContext(hits);
        log.info("Build prompt and get context : {}", context.length());
        String rawAnswer = generateAnswer(query, context);
        log.info("Build prompt and generate answer : {}", rawAnswer.length());

        // Step 6: Sanitize output
        log.info("Processing buildContext:");
        String safeAnswer = biasCheckerService.sanitizeOutput(rawAnswer);
        log.info("Sanitized output : {}", safeAnswer.length());
        if (biasDetected) {
            safeAnswer = biasCheckerService.getBiasWarning() + "\n\n" + safeAnswer;
        }

        // Step 7: Audit log (mandatory for compliance)
        log.info("Processing buildContext:");
        AuditLog auditLog = auditLogRepository.save(AuditLog.builder()
            .tenantId(defaultTenantId)
            .queryText(query)
            .matchedIds(matches.stream()
                .map(m -> m.getCandidateId().toString())
                .collect(Collectors.joining(",")))
            .biasDetected(biasDetected)
            .modelUsed(chatModelName)
            .build());
        log.info("AuditLog output : {}", auditLog);

        // Step 8: Return response
        return RagQueryResponse.builder()
            .query(query)
            .aiAnswer(safeAnswer)
            .matches(matches)
            .biasDetected(biasDetected)
            .auditLogId(auditLog.getId().toString())
            .build();
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private List<ScoredPoint> searchQdrant(float[] queryVector, int topK) {
        try {
            SearchPoints searchRequest = SearchPoints.newBuilder()
                .setCollectionName(resumeCollection)
                .addAllVector(toFloatList(queryVector))
                .setLimit(topK)
                .setWithPayload(
                    io.qdrant.client.grpc.Points.WithPayloadSelector.newBuilder()
                        .setEnable(true).build()
                )
                .build();

            return qdrantClient.searchAsync(searchRequest).get();
        } catch (Exception e) {
            log.error("Qdrant search failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<CandidateMatch> buildMatches(List<ScoredPoint> hits) {

        // ── Step A: Collect all candidate IDs from all hits at once ──
        Map<UUID, String> idToChunkText = new LinkedHashMap<>();

        for (ScoredPoint hit : hits) {
            Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload = hit.getPayloadMap();
            String candidateIdStr = payload.containsKey("candidateId")
                    ? payload.get("candidateId").getStringValue() : null;
            if (candidateIdStr == null) continue;

            UUID candidateId = UUID.fromString(candidateIdStr);
            String chunkText = payload.containsKey("text")
                    ? payload.get("text").getStringValue() : "";
            idToChunkText.put(candidateId, chunkText);
        }

        // ── Step B: ONE single DB query for all candidates ──
        // SELECT * FROM candidates WHERE id IN ('id-1', 'id-2', 'id-3', ...)
        Map<UUID, Candidate> candidateMap = candidateRepository
                .findAllById(idToChunkText.keySet())   // 1 query only
                .stream()
                .collect(Collectors.toMap(Candidate::getId, c -> c));

        // ── Step C: Build matches using the in-memory map (no DB calls) ──
        List<CandidateMatch> matches = new ArrayList<>();

        for (ScoredPoint hit : hits) {
            Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload = hit.getPayloadMap();
            String candidateIdStr = payload.containsKey("candidateId")
                    ? payload.get("candidateId").getStringValue() : null;
            if (candidateIdStr == null) continue;

            UUID candidateId = UUID.fromString(candidateIdStr);
            String chunkText = idToChunkText.getOrDefault(candidateId, "");

            // No DB call — just a map lookup (instant)
            String name = candidateMap.containsKey(candidateId)
                    ? candidateMap.get(candidateId).getFullName()
                    : "Unknown";

            matches.add(CandidateMatch.builder()
                    .candidateId(candidateId)
                    .name(name)
                    .similarityScore(hit.getScore())
                    .relevantExcerpt(truncate(chunkText, 300))
                    .build());
        }

        return matches;
    }

    /**
     * Builds the context string from retrieved chunks.
     * This is injected into the prompt as "what the LLM knows".
     */
    private String buildContext(List<ScoredPoint> hits) {
        StringBuilder context = new StringBuilder();
        int i = 1;
        for (ScoredPoint hit : hits) {
            Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload = hit.getPayloadMap();
            String section = payload.containsKey("section") ? payload.get("section").getStringValue() : "N/A";
            String text = payload.containsKey("text") ? payload.get("text").getStringValue() : "";
            context.append(String.format("[Candidate %d - %s] (score: %.2f)\n%s\n\n",
                i++, section, hit.getScore(), text));
        }
        return context.toString();
    }

    /**
     * Calls Ollama (llama3.1:8b) to generate a structured recruiter answer.
     *
     * The prompt is carefully structured to:
     * - Constrain the model to use ONLY the provided context (no hallucination)
     * - Request structured output the recruiter can act on
     * - Enforce transparency (mention it's AI-generated)
     */
    private String generateAnswer(String query, String context) {
        String promptText = """
        Recruitment assistant. Use ONLY the CONTEXT. Be brief and direct.

        CONTEXT:
        %s

        QUERY: %s

        Answer in this exact format:
        1. [Candidate Name] - [2-3 skills only]
        2. [Candidate Name] - [2-3 skills only]
        Ranking: [Name] is best because [one sentence].
        ⚠ AI-assisted screening. Human review required.
        """.formatted(context, query);

        try {
            return chatModel.call(new Prompt(promptText))
                    .getResult()
                    .getOutput()
                    .getText();
        } catch (Exception e) {
            log.error("Ollama generation failed: {}", e.getMessage());
            return "AI generation failed. Please review the matched candidates manually.";
        }
    }

    private List<Float> toFloatList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float f : array) list.add(f);
        return list;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
