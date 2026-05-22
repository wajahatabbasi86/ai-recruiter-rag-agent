package com.airecruiter.service;

import com.airecruiter.dto.RecruiterDtos.CandidateUpdateRequest;
import com.airecruiter.model.Candidate;
import com.airecruiter.repository.CandidateRepository;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points.PointId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static io.qdrant.client.PointIdFactory.id;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final ResumeParserService parserService;
    private final EmbeddingService embeddingService;
    private final QdrantClient qdrantClient;

    @Value("${app.default-tenant-id}")
    private UUID defaultTenantId;

    @Value("${qdrant.collections.resumes}")
    private String resumeCollection;

    /**
     * Full onboarding flow for a new candidate resume:
     * 1. Save candidate profile to PostgreSQL
     * 2. Extract text from the resume file (Apache Tika)
     * 3. Chunk text into sections
     * 4. Embed and store chunks in Qdrant (async)
     * 5. Persist all chunk IDs back to Postgres for future cleanup
     */
    public Candidate ingestResume(MultipartFile file, String candidateName) throws IOException, TikaException, SAXException, ExecutionException, InterruptedException {
        log.info("Ingesting resume for: {}", candidateName);

        String rawText = parserService.extractText(file);

        Candidate candidate = Candidate.builder()
            .tenantId(defaultTenantId)
            .fullName(candidateName)
            .source(Candidate.CandidateSource.RESUME)
            .rawText(rawText)
            .build();
        candidate = candidateRepository.save(candidate);

        var chunks = parserService.chunk(rawText);
        List<String> pointIds = embeddingService.embedAndStoreResume(candidate.getId(), chunks).get();

        // Persist all chunk point IDs so we can clean them up on delete
        candidate.setQdrantPointIds(String.join(",", pointIds));
        candidateRepository.save(candidate);

        log.info("Candidate {} created with id {} and {} Qdrant vectors",
            candidateName, candidate.getId(), pointIds.size());
        return candidate;
    }

    public List<Candidate> getAllCandidates() {
        return candidateRepository.findByTenantId(defaultTenantId);
    }

    public Candidate getById(UUID id) {
        return candidateRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Candidate not found: " + id));
    }

    /**
     * Update mutable profile fields (name, email, phone, source).
     * Does NOT re-embed — call ingestResume again if the resume itself changes.
     */
    public Candidate update(UUID id, CandidateUpdateRequest req) {
        Candidate candidate = getById(id);
        if (req.getFullName() != null) candidate.setFullName(req.getFullName());
        if (req.getEmail()    != null) candidate.setEmail(req.getEmail());
        if (req.getPhone()    != null) candidate.setPhone(req.getPhone());
        if (req.getSource()   != null) candidate.setSource(
            Candidate.CandidateSource.valueOf(req.getSource().toUpperCase()));
        log.info("Updated candidate {}", id);
        return candidateRepository.save(candidate);
    }

    /**
     * Delete candidate profile AND all associated Qdrant vectors.
     */
    public void delete(UUID id) {
        Candidate candidate = getById(id);

        if (candidate.getQdrantPointIds() != null && !candidate.getQdrantPointIds().isBlank()) {
            List<PointId> pointIds = Arrays.stream(candidate.getQdrantPointIds().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> id(UUID.fromString(s)))
                .collect(Collectors.toList());
            try {
                qdrantClient.deleteAsync(resumeCollection, pointIds).get();
                log.info("Deleted {} Qdrant vectors for candidate {}", pointIds.size(), id);
            } catch (Exception e) {
                log.error("Failed to delete Qdrant vectors for candidate {}: {}", id, e.getMessage());
            }
        }

        candidateRepository.deleteById(id);
        log.info("Deleted candidate {}", id);
    }
}
