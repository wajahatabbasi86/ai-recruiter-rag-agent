package com.airecruiter.controller;

import com.airecruiter.dto.RecruiterDtos.*;
import com.airecruiter.model.Candidate;
import com.airecruiter.service.CandidateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for resume ingestion and candidate profile management.
 *
 * POST   /api/resumes/upload      — upload a resume file
 * GET    /api/candidates          — list all candidates
 * GET    /api/candidates/{id}     — get one candidate
 * PUT    /api/candidates/{id}     — update candidate profile fields
 * DELETE /api/candidates/{id}     — delete candidate + Qdrant vectors
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ResumeController {

    private final CandidateService candidateService;

    /**
     * Upload a resume file and trigger the full ingestion pipeline.
     *
     * curl -X POST http://localhost:8080/api/resumes/upload \
     *   -F "file=@resume.pdf" \
     *   -F "candidateName=John Doe"
     */
    @PostMapping("/resumes/upload")
    public ResponseEntity<ResumeUploadResponse> uploadResume(
        @RequestParam("file") MultipartFile file,
        @RequestParam("candidateName") String candidateName
    ) {
        try {
            Candidate candidate = candidateService.ingestResume(file, candidateName);
            return ResponseEntity.ok(ResumeUploadResponse.builder()
                .candidateId(candidate.getId())
                .name(candidate.getFullName())
                .message("Resume ingested. Embedding in progress asynchronously.")
                .build());
        } catch (Exception e) {
            log.error("Resume upload failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/candidates")
    public ResponseEntity<List<Candidate>> getAllCandidates() {
        return ResponseEntity.ok(candidateService.getAllCandidates());
    }

    @GetMapping("/candidates/{id}")
    public ResponseEntity<Candidate> getCandidate(@PathVariable UUID id) {
        return ResponseEntity.ok(candidateService.getById(id));
    }

    /**
     * Update mutable profile fields (name, email, phone, source).
     * Does NOT re-embed — re-upload the resume if content changes.
     *
     * curl -X PUT http://localhost:8080/api/candidates/{id} \
     *   -H "Content-Type: application/json" \
     *   -d '{"email":"john@example.com","phone":"+923001234567"}'
     */
    @PutMapping("/candidates/{id}")
    public ResponseEntity<Candidate> updateCandidate(
        @PathVariable UUID id,
        @RequestBody CandidateUpdateRequest request
    ) {
        return ResponseEntity.ok(candidateService.update(id, request));
    }

    @DeleteMapping("/candidates/{id}")
    public ResponseEntity<Void> deleteCandidate(@PathVariable UUID id) {
        candidateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
