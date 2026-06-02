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

    /**
     * Upload a resume file.
     * POST /api/candidates/upload
     *
     * @param resumeFile the resume file
     * @param candidateName the candidate name
     * @return response entity with candidate response DTO
     */
    @PostMapping("/upload")
    public ResponseEntity<CandidateResponseDto> uploadResume(
            @RequestParam("file") MultipartFile resumeFile,
            @RequestParam("candidateName") String candidateName) {
        
        log.info("Resume upload request received for candidate: {}", candidateName);
        
        try {
            CandidateResponseDto responseDto = candidateService.ingestResume(resumeFile, candidateName);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (IOException ioException) {
            log.error("IO error during resume upload: {}", ioException.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (TikaException tikaException) {
            log.error("Tika parsing error during resume upload: {}", tikaException.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception exception) {
            log.error("Error during resume upload: {}", exception.getMessage(), exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all candidates.
     * GET /api/candidates
     *
     * @return response entity with list of candidate response DTOs
     */
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

    /**
     * Get a specific candidate by ID.
     * GET /api/candidates/{candidateId}
     *
     * @param candidateId the candidate ID
     * @return response entity with candidate response DTO
     */
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

    /**
     * Delete a candidate.
     * DELETE /api/candidates/{candidateId}
     *
     * @param candidateId the candidate ID
     * @return response entity
     */
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
