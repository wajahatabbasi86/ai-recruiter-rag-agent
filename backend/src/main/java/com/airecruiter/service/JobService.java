package com.airecruiter.service;

import com.airecruiter.model.Job;
import com.airecruiter.repository.JobRepository;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points.PointId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.qdrant.client.PointIdFactory.id;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final JobParserService parserService;
    private final EmbeddingService embeddingService;
    private final QdrantClient qdrantClient;

    @Value("${app.default-tenant-id}")
    private UUID defaultTenantId;

    @Value("${qdrant.collections.jobs}")
    private String jobCollection;

    /**
     * Ingest a job description from an uploaded file.
     * Mirrors CandidateService.ingestResume() exactly.
     */
    public Job ingestJobFile(MultipartFile file, String title) throws Exception {
        log.info("Ingesting job description file for: {}", title);
        String rawText = parserService.extractText(file);
        return ingest(title, rawText);
    }

    /**
     * Ingest a job description from plain text (e.g. pasted into a form).
     */
    public Job ingestJobText(String title, String description) throws Exception {
        log.info("Ingesting job description text for: {}", title);
        return ingest(title, description);
    }

    private Job ingest(String title, String rawText) throws Exception {
        Job job = Job.builder()
            .tenantId(defaultTenantId)
            .title(title)
            .rawText(rawText)
            .build();
        job = jobRepository.save(job);

        var chunks = parserService.chunk(rawText);
        List<String> pointIds = embeddingService.embedAndStoreJob(job.getId(), chunks).get();

        // Persist chunk IDs so we can clean up Qdrant on delete
        job.setQdrantPointIds(String.join(",", pointIds));
        jobRepository.save(job);

        log.info("Job {} created with {} Qdrant vectors", title, pointIds.size());
        return job;
    }

    public List<Job> getAllJobs() {
        return jobRepository.findByTenantId(defaultTenantId);
    }

    public Job getById(UUID id) {
        return jobRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Job not found: " + id));
    }

    public void delete(UUID id) {
        Job job = getById(id);

        // Delete Qdrant vectors for this job
        if (job.getQdrantPointIds() != null && !job.getQdrantPointIds().isBlank()) {
            List<PointId> pointIds = Arrays.stream(job.getQdrantPointIds().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> id(UUID.fromString(s)))
                .collect(Collectors.toList());
            try {
                qdrantClient.deleteAsync(jobCollection, pointIds).get();
                log.info("Deleted {} Qdrant vectors for job {}", pointIds.size(), id);
            } catch (Exception e) {
                log.error("Failed to delete Qdrant vectors for job {}: {}", id, e.getMessage());
            }
        }

        jobRepository.deleteById(id);
        log.info("Deleted job {}", id);
    }
}
