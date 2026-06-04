package com.airecruiter.controller;

import com.airecruiter.dto.JobRequestDto;
import com.airecruiter.dto.JobResponseDto;
import com.airecruiter.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Job Description management.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping("/upload")
    public ResponseEntity<JobResponseDto> uploadJobDescriptionFile(
            @RequestParam("file") MultipartFile jobFile,
            @RequestParam("jobTitle") String jobTitle) {

        log.info("Job description file upload request received for job: {}", jobTitle);

        try {
            JobResponseDto responseDto = jobService.uploadJobDescriptionFile(jobFile, jobTitle);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (IOException ioException) {
            log.error("IO error during job file upload: {}", ioException.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (TikaException tikaException) {
            log.error("Tika parsing error during job file upload: {}", tikaException.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception exception) {
            log.error("Error during job file upload: {}", exception.getMessage(), exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // FIX 5: Added @Valid to trigger validation on JobRequestDto
    @PostMapping
    public ResponseEntity<?> createJobDescription(@Valid @RequestBody JobRequestDto jobRequestDto) {
        log.info("Job description creation request received for job: {}", jobRequestDto.getJobTitle());

        try {
            JobResponseDto responseDto = jobService.createJobDescription(jobRequestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (Exception exception) {
            log.error("Error during job creation: {}", exception.getMessage(), exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<JobResponseDto>> getAllActiveJobs() {
        log.info("Request received to fetch all active jobs");

        try {
            List<JobResponseDto> jobs = jobService.getAllActiveJobs();
            return ResponseEntity.ok(jobs);
        } catch (Exception exception) {
            log.error("Error fetching all jobs: {}", exception.getMessage(), exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<JobResponseDto> getJobById(@PathVariable UUID jobId) {
        log.info("Request received to fetch job with ID: {}", jobId);

        try {
            JobResponseDto responseDto = jobService.getJobById(jobId);
            return ResponseEntity.ok(responseDto);
        } catch (RuntimeException runtimeException) {
            log.error("Job not found: {}", jobId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception exception) {
            log.error("Error fetching job: {}", exception.getMessage(), exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{jobId}")
    public ResponseEntity<JobResponseDto> updateJobDescription(
            @PathVariable UUID jobId,
            @Valid @RequestBody JobRequestDto jobRequestDto) {

        log.info("Request received to update job with ID: {}", jobId);

        try {
            JobResponseDto responseDto = jobService.updateJobDescription(jobId, jobRequestDto);
            return ResponseEntity.ok(responseDto);
        } catch (RuntimeException runtimeException) {
            log.error("Job not found: {}", jobId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception exception) {
            log.error("Error updating job: {}", exception.getMessage(), exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> deleteJob(@PathVariable UUID jobId) {
        log.info("Request received to delete job with ID: {}", jobId);

        try {
            jobService.deleteJob(jobId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException runtimeException) {
            log.error("Job not found: {}", jobId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception exception) {
            log.error("Error deleting job: {}", exception.getMessage(), exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
