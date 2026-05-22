package com.airecruiter.controller;

import com.airecruiter.model.Job;
import com.airecruiter.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for job description ingestion and management.
 *
 * POST /api/jobs/upload          — upload a JD file
 * POST /api/jobs/text            — ingest JD as plain text
 * GET  /api/jobs                 — list all jobs
 * GET  /api/jobs/{id}            — get one job
 * DELETE /api/jobs/{id}          — delete job + Qdrant vectors
 */
@Slf4j
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    /**
     * Upload a JD file (PDF, DOCX, TXT).
     *
     * curl -X POST http://localhost:8080/api/jobs/upload \
     *   -F "file=@job_description.pdf" \
     *   -F "title=Senior Java Developer"
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadJobFile(
        @RequestParam("file") MultipartFile file,
        @RequestParam("title") String title
    ) {
        try {
            Job job = jobService.ingestJobFile(file, title);
            return ResponseEntity.ok(Map.of(
                "jobId",     job.getId(),
                "title",     job.getTitle(),
                "message",   "Job description ingested. Embedding in progress.",
                "processedBy", "AI-assisted (nomic-embed-text via Ollama)"
            ));
        } catch (Exception e) {
            log.error("Job file upload failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to ingest job description"));
        }
    }

    /**
     * Ingest a job description as plain text (e.g. from a web form).
     *
     * curl -X POST http://localhost:8080/api/jobs/text \
     *   -H "Content-Type: application/json" \
     *   -d '{"title":"Backend Engineer","description":"We are looking for..."}'
     */
    @PostMapping("/text")
    public ResponseEntity<?> ingestJobText(@RequestBody Map<String, String> body) {
        String title = body.get("title");
        String description = body.get("description");
        if (title == null || description == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Both 'title' and 'description' are required"));
        }
        try {
            Job job = jobService.ingestJobText(title, description);
            return ResponseEntity.ok(Map.of(
                "jobId",   job.getId(),
                "title",   job.getTitle(),
                "message", "Job description ingested. Embedding in progress."
            ));
        } catch (Exception e) {
            log.error("Job text ingest failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to ingest job description"));
        }
    }

    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs() {
        return ResponseEntity.ok(jobService.getAllJobs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJob(@PathVariable UUID id) {
        return ResponseEntity.ok(jobService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable UUID id) {
        jobService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
