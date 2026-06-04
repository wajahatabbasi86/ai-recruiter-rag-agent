package com.airecruiter.controller;

import com.airecruiter.dto.CandidateResponseDto;
import com.airecruiter.service.CandidateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * REST Controller for Candidate profile management.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/candidates")
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateService candidateService;

    // FIX 6: Allowed file types for resume uploads
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "text/plain"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".docx", ".doc", ".txt");

    @PostMapping("/upload")
    public ResponseEntity<?> uploadResume(
            @RequestParam("file") MultipartFile resumeFile,
            @RequestParam("candidateName") String candidateName) {

        log.info("Resume upload request received for candidate: {}", candidateName);

        // FIX 6: Validate file is not empty
        if (resumeFile.isEmpty()) {
            return ResponseEntity.badRequest().body("File must not be empty");
        }

        // FIX 6: Validate candidate name
        if (candidateName == null || candidateName.isBlank()) {
            return ResponseEntity.badRequest().body("Candidate name is required");
        }

        // FIX 6: Validate file type by content type and extension
        String contentType = resumeFile.getContentType();
        String originalFilename = resumeFile.getOriginalFilename() != null
                ? resumeFile.getOriginalFilename().toLowerCase() : "";
        boolean validExtension = ALLOWED_EXTENSIONS.stream().anyMatch(originalFilename::endsWith);
        boolean validContentType = contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType);

        if (!validExtension && !validContentType) {
            return ResponseEntity.badRequest()
                    .body("Invalid file type. Allowed: PDF, DOCX, DOC, TXT");
        }

        try {
            CandidateResponseDto responseDto = candidateService.ingestResume(resumeFile, candidateName.trim());
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (IOException ioException) {
            log.error("IO error during resume upload: {}", ioException.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to read the uploaded file");
        } catch (TikaException tikaException) {
            log.error("Tika parsing error during resume upload: {}", tikaException.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to parse the uploaded file");
        } catch (Exception exception) {
            log.error("Error during resume upload: {}", exception.getMessage(), exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred");
        }
    }

    @GetMapping
    public ResponseEntity<List<CandidateResponseDto>> getAllCandidates() {
        log.info("Request received to fetch all candidates");

        try {
            List<CandidateResponseDto> candidates = candidateService.getAllCandidates();
            return ResponseEntity.ok(candidates);
        } catch (Exception exception) {
            log.error("Error fetching all candidates: {}", exception.getMessage(), exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{candidateId}")
    public ResponseEntity<CandidateResponseDto> getCandidateById(@PathVariable UUID candidateId) {
        log.info("Request received to fetch candidate with ID: {}", candidateId);

        try {
            CandidateResponseDto responseDto = candidateService.getCandidateById(candidateId);
            return ResponseEntity.ok(responseDto);
        } catch (RuntimeException runtimeException) {
            log.error("Candidate not found: {}", candidateId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception exception) {
            log.error("Error fetching candidate: {}", exception.getMessage(), exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{candidateId}")
    public ResponseEntity<Void> deleteCandidate(@PathVariable UUID candidateId) {
        log.info("Request received to delete candidate with ID: {}", candidateId);

        try {
            candidateService.deleteCandidate(candidateId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException runtimeException) {
            log.error("Candidate not found: {}", candidateId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception exception) {
            log.error("Error deleting candidate: {}", exception.getMessage(), exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
