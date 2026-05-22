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
@RequiredArgsConstructor
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

    @Value("${spring.ai.ollama.chat.model}")
    private String chatModelName;

    /**
     * Main entry point: processes a recruiter's natural language query.
     */
    public RagQueryResponse query(RagQueryRequest request) {
        int topK = request.getTopK() > 0 ? request.getTopK() : defaultTopK;
        String query = request.getQuery();

        log.info("Processing RAG query: '{}'", query);

        // Step 1: Bias check
        boolean biasDetected = biasCheckerService.detectBiasInQuery(query);

        // Step 2: Embed the query
        float[] queryVector = embeddingService.embedQuery(query);

        // Step 3: Retrieve top-K chunks from Qdrant
        List<ScoredPoint> hits = searchQdrant(queryVector, topK);

        // Step 4: Extract text from results and look up candidate metadata
        List<CandidateMatch> matches = buildMatches(hits);

        // Step 5: Build prompt and generate answer
        String context = buildContext(hits);
        String rawAnswer = generateAnswer(query, context);

        // Step 6: Sanitize output
        String safeAnswer = biasCheckerService.sanitizeOutput(rawAnswer);
        if (biasDetected) {
            safeAnswer = biasCheckerService.getBiasWarning() + "\n\n" + safeAnswer;
        }

        // Step 7: Audit log (mandatory for compliance)
        AuditLog auditLog = auditLogRepository.save(AuditLog.builder()
            .tenantId(defaultTenantId)
            .queryText(query)
            .matchedIds(matches.stream()
                .map(m -> m.getCandidateId().toString())
                .collect(Collectors.joining(",")))
            .biasDetected(biasDetected)
            .modelUsed(chatModelName)
            .build());

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
        List<CandidateMatch> matches = new ArrayList<>();

        for (ScoredPoint hit : hits) {
            Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload = hit.getPayloadMap();

            String candidateIdStr = payload.containsKey("candidateId")
                ? payload.get("candidateId").getStringValue() : null;
            String chunkText = payload.containsKey("text")
                ? payload.get("text").getStringValue() : "";

            if (candidateIdStr == null) continue;

            UUID candidateId = UUID.fromString(candidateIdStr);
            Optional<Candidate> candidateOpt = candidateRepository.findById(candidateId);

            CandidateMatch match = CandidateMatch.builder()
                .candidateId(candidateId)
                .name(candidateOpt.map(Candidate::getFullName).orElse("Unknown"))
                .similarityScore(hit.getScore())
                .relevantExcerpt(truncate(chunkText, 300))
                .build();

            matches.add(match);
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
            You are an AI recruitment assistant. Your job is to help recruiters
            find the best candidates based on their resume content.
            
            RULES:
            - Only use information from the CONTEXT provided below.
            - Do NOT make up or infer information not present in the context.
            - Focus on skills, experience, and qualifications only.
            - Do NOT comment on or infer personal characteristics (gender, age, ethnicity).
            - Be concise and factual.
            - Always note this is AI-assisted screening requiring human review.
            
            CONTEXT (retrieved resume sections):
            %s
            
            RECRUITER QUERY: %s
            
            Provide a structured answer with:
            1. A summary of the most relevant candidates
            2. Key matching skills/experience for each
            3. A recommended ranking with brief justification
            
            End with: "⚠ This is AI-assisted screening. Human review is required before any hiring decision."
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
