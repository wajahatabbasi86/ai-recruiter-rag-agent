package com.airecruiter.service;

import com.airecruiter.dto.JobRequestDto;
import com.airecruiter.dto.JobResponseDto;
import com.airecruiter.model.Job;
import com.airecruiter.repository.JobRepository;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points.PointId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.qdrant.client.PointIdFactory.id;

/**
 * Service for job description management.
 * Handles uploading, extracting, and storing job descriptions.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final ResumeExtractionService resumeExtractionService;
    private final EmbeddingService embeddingService;
    private final QdrantClient qdrantClient;

    @Value("${app.default-tenant-id}")
    private UUID defaultTenantId;

    @Value("${qdrant.collections.jobs}")
    private String jobsCollection;

    /**
     * Uploads and processes a job description file.
     *
     * @param jobFile the job description file
     * @param jobTitle the job title
     * @return job response DTO
     * @throws IOException if file reading fails
     * @throws TikaException if parsing fails
     */
    public JobResponseDto uploadJobDescriptionFile(MultipartFile jobFile, String jobTitle)
            throws IOException, TikaException {
        
        log.info("Uploading job description file for job: {}", jobTitle);
        
        String rawJobText = resumeExtractionService.extractTextFromFile(jobFile);
        
        Job jobEntity = Job.builder()
                .tenantId(defaultTenantId)
                .jobTitle(jobTitle)
                .originalFilePath(jobFile.getOriginalFilename())
                .rawExtractedText(rawJobText)
                .jobStatus("active")
                .build();
        
        jobEntity = jobRepository.save(jobEntity);
        
        log.info("Job description saved with ID: {}", jobEntity.getId());
        
        return mapJobToResponseDto(jobEntity, "Job description uploaded successfully");
    }

    /**
     * Creates a job description from text input.
     *
     * @param jobRequestDto the job request DTO
     * @return job response DTO
     */
    public JobResponseDto createJobDescription(JobRequestDto jobRequestDto) {
        log.info("Creating job description: {}", jobRequestDto.getJobTitle());
        
        Job jobEntity = Job.builder()
                .tenantId(defaultTenantId)
                .jobTitle(jobRequestDto.getJobTitle())
                .jobDescription(jobRequestDto.getJobDescription())
                .requiredSkills(jobRequestDto.getRequiredSkills())
                .minimumExperienceYears(jobRequestDto.getMinimumExperienceYears())
                .preferredExperienceYears(jobRequestDto.getPreferredExperienceYears())
                .requiredEducationLevel(jobRequestDto.getRequiredEducationLevel())
                .requiredLocation(jobRequestDto.getRequiredLocation())
                .requiredTools(jobRequestDto.getRequiredTools())
                .requiredFrameworks(jobRequestDto.getRequiredFrameworks())
                .requiredDatabases(jobRequestDto.getRequiredDatabases())
                .jobStatus("active")
                .build();
        
        jobEntity = jobRepository.save(jobEntity);
        
        log.info("Job description created with ID: {}", jobEntity.getId());
        
        return mapJobToResponseDto(jobEntity, "Job description created successfully");
    }

    /**
     * Retrieves all active jobs for the tenant.
     *
     * @return list of job response DTOs
     */
    public List<JobResponseDto> getAllActiveJobs() {
        log.debug("Fetching all active jobs for tenant");
        
        return jobRepository.findByTenantIdAndJobStatus(defaultTenantId, "active")
                .stream()
                .map(job -> mapJobToResponseDto(job, null))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a specific job by ID.
     *
     * @param jobId the job ID
     * @return job response DTO
     */
    public JobResponseDto getJobById(UUID jobId) {
        log.debug("Fetching job with ID: {}", jobId);
        
        Job jobEntity = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        
        return mapJobToResponseDto(jobEntity, null);
    }

    /**
     * Updates a job description.
     *
     * @param jobId the job ID
     * @param jobRequestDto the job request DTO
     * @return updated job response DTO
     */
    public JobResponseDto updateJobDescription(UUID jobId, JobRequestDto jobRequestDto) {
        log.info("Updating job description: {}", jobId);
        
        Job jobEntity = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        
        if (jobRequestDto.getJobTitle() != null) {
            jobEntity.setJobTitle(jobRequestDto.getJobTitle());
        }
        if (jobRequestDto.getJobDescription() != null) {
            jobEntity.setJobDescription(jobRequestDto.getJobDescription());
        }
        if (jobRequestDto.getRequiredSkills() != null) {
            jobEntity.setRequiredSkills(jobRequestDto.getRequiredSkills());
        }
        if (jobRequestDto.getMinimumExperienceYears() != null) {
            jobEntity.setMinimumExperienceYears(jobRequestDto.getMinimumExperienceYears());
        }
        if (jobRequestDto.getRequiredEducationLevel() != null) {
            jobEntity.setRequiredEducationLevel(jobRequestDto.getRequiredEducationLevel());
        }
        if (jobRequestDto.getRequiredLocation() != null) {
            jobEntity.setRequiredLocation(jobRequestDto.getRequiredLocation());
        }
        if (jobRequestDto.getRequiredTools() != null) {
            jobEntity.setRequiredTools(jobRequestDto.getRequiredTools());
        }
        if (jobRequestDto.getRequiredFrameworks() != null) {
            jobEntity.setRequiredFrameworks(jobRequestDto.getRequiredFrameworks());
        }
        if (jobRequestDto.getRequiredDatabases() != null) {
            jobEntity.setRequiredDatabases(jobRequestDto.getRequiredDatabases());
        }
        
        jobEntity = jobRepository.save(jobEntity);
        
        log.info("Job description updated: {}", jobId);
        
        return mapJobToResponseDto(jobEntity, "Job description updated successfully");
    }

    /**
     * Deletes a job description and its associated vectors.
     *
     * @param jobId the job ID
     */
    public void deleteJob(UUID jobId) {
        log.info("Deleting job with ID: {}", jobId);
        
        Job jobEntity = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        
        // Delete Qdrant vectors
        if (jobEntity.getQdrantPointIds() != null && !jobEntity.getQdrantPointIds().isBlank()) {
            List<PointId> pointIds = Arrays.stream(jobEntity.getQdrantPointIds().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> id(UUID.fromString(s)))
                    .collect(Collectors.toList());
            
            try {
                qdrantClient.deleteAsync(jobsCollection, pointIds).get();
                log.debug("Deleted {} Qdrant vectors for job: {}", pointIds.size(), jobId);
            } catch (Exception exception) {
                log.error("Failed to delete Qdrant vectors for job {}: {}", jobId, exception.getMessage());
            }
        }
        
        jobRepository.deleteById(jobId);
        log.info("Job deleted successfully: {}", jobId);
    }

    /**
     * Maps Job entity to JobResponseDto.
     *
     * @param jobEntity the job entity
     * @param message optional message
     * @return job response DTO
     */
    private JobResponseDto mapJobToResponseDto(Job jobEntity, String message) {
        return JobResponseDto.builder()
                .jobId(jobEntity.getId())
                .jobTitle(jobEntity.getJobTitle())
                .jobDescription(jobEntity.getJobDescription())
                .requiredSkills(jobEntity.getRequiredSkills())
                .minimumExperienceYears(jobEntity.getMinimumExperienceYears())
                .preferredExperienceYears(jobEntity.getPreferredExperienceYears())
                .requiredEducationLevel(jobEntity.getRequiredEducationLevel())
                .requiredLocation(jobEntity.getRequiredLocation())
                .requiredTools(jobEntity.getRequiredTools())
                .requiredFrameworks(jobEntity.getRequiredFrameworks())
                .requiredDatabases(jobEntity.getRequiredDatabases())
                .jobStatus(jobEntity.getJobStatus())
                .createdAt(jobEntity.getCreatedAt())
                .updatedAt(jobEntity.getUpdatedAt())
                .message(message)
                .build();
    }
}
