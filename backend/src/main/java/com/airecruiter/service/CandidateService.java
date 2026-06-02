package com.airecruiter.service;

import com.airecruiter.dto.CandidateResponseDto;
import com.airecruiter.dto.ResumeMetadataDto;
import com.airecruiter.model.Candidate;
import com.airecruiter.model.ResumeMetadata;
import com.airecruiter.repository.CandidateRepository;
import com.airecruiter.repository.ResumeMetadataRepository;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points.PointId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static io.qdrant.client.PointIdFactory.id;

/**
 * Service for candidate profile management.
 * Handles resume uploads, extraction, and candidate profile operations.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final ResumeMetadataRepository resumeMetadataRepository;
    private final ResumeExtractionService resumeExtractionService;
    private final EmbeddingService embeddingService;
    private final DuplicateDetectionService duplicateDetectionService;
    private final QdrantClient qdrantClient;

    @Value("${app.default-tenant-id}")
    private UUID defaultTenantId;

    @Value("${qdrant.collections.resumes}")
    private String resumesCollection;

    /**
     * Ingests a resume file and creates candidate profile asynchronously.
     *
     * @param resumeFile the resume file
     * @param candidateName the candidate name
     * @return candidate response DTO
     * @throws IOException if file reading fails
     * @throws TikaException if parsing fails
     */
    public CandidateResponseDto ingestResume(MultipartFile resumeFile, String candidateName)
            throws IOException, TikaException {
        
        log.info("Ingesting resume for candidate: {}", candidateName);
        
        // Extract text from resume
        String rawResumeText = resumeExtractionService.extractTextFromFile(resumeFile);
        
        // Create candidate profile
        Candidate candidateEntity = Candidate.builder()
                .tenantId(defaultTenantId)
                .fullName(candidateName)
                .source("RESUME")
                .rawText(rawResumeText)
                .build();
        
        candidateEntity = candidateRepository.save(candidateEntity);
        log.debug("Candidate profile created with ID: {}", candidateEntity.getId());
        
        // Extract structured metadata asynchronously
        ResumeMetadataDto metadataDto = resumeExtractionService.extractStructuredMetadata(rawResumeText);
        
        ResumeMetadata resumeMetadata = ResumeMetadata.builder()
                .candidateId(candidateEntity.getId())
                .technicalSkills(metadataDto.getTechnicalSkills())
                .totalExperienceYears(metadataDto.getTotalExperienceYears())
                .previousCompanies(metadataDto.getPreviousCompanies())
                .jobTitlesHeld(metadataDto.getJobTitlesHeld())
                .educationLevel(metadataDto.getEducationLevel())
                .universityName(metadataDto.getUniversityName())
                .fieldsOfStudy(metadataDto.getFieldsOfStudy())
                .currentLocation(metadataDto.getCurrentLocation())
                .country(metadataDto.getCountry())
                .willingToRelocate(metadataDto.getWillingToRelocate())
                .tools(metadataDto.getTools())
                .frameworks(metadataDto.getFrameworks())
                .databases(metadataDto.getDatabases())
                .latestCompanyName(metadataDto.getLatestCompanyName())
                .latestJobTitle(metadataDto.getLatestJobTitle())
                .latestRoleEndDate(metadataDto.getLatestRoleEndDate())
                .currentEmploymentStatus(metadataDto.getCurrentEmploymentStatus())
                .extractedEmail(metadataDto.getExtractedEmail())
                .extractedPhone(metadataDto.getExtractedPhone())
                .linkedInProfileUrl(metadataDto.getLinkedInProfileUrl())
                .build();
        
        resumeMetadata = resumeMetadataRepository.save(resumeMetadata);
        log.debug("Resume metadata created with ID: {}", resumeMetadata.getId());
        
        // Update candidate with metadata contact info
        if (metadataDto.getExtractedEmail() != null) {
            candidateEntity.setEmailAddress(metadataDto.getExtractedEmail());
        }
        if (metadataDto.getExtractedPhone() != null) {
            candidateEntity.setPhoneNumber(metadataDto.getExtractedPhone());
        }
        candidateEntity = candidateRepository.save(candidateEntity);
        
        // Detect duplicates
        embeddingAndDuplicateDetection(candidateEntity.getId());
        
        log.info("Resume ingested successfully for candidate: {}", candidateName);
        
        return mapCandidateToResponseDto(candidateEntity);
    }

    /**
     * Performs embedding and duplicate detection asynchronously.
     *
     * @param candidateId the candidate ID
     */
    @Async
    public void embeddingAndDuplicateDetection(UUID candidateId) {
        try {
            // Perform embedding
            log.debug("Starting embedding process for candidate: {}", candidateId);
            
            // Detect duplicates
            List<com.airecruiter.model.DuplicateCandidate> potentialDuplicates =
                    duplicateDetectionService.detectDuplicates(candidateId, defaultTenantId);
            
            if (!potentialDuplicates.isEmpty()) {
                log.warn("Found {} potential duplicate(s) for candidate: {}",
                        potentialDuplicates.size(), candidateId);
            }
            
        } catch (Exception exception) {
            log.error("Error during embedding or duplicate detection for candidate {}: {}",
                    candidateId, exception.getMessage(), exception);
        }
    }

    /**
     * Retrieves all candidates for the tenant.
     *
     * @return list of candidate response DTOs
     */
    public List<CandidateResponseDto> getAllCandidates() {
        log.debug("Fetching all candidates for tenant");
        
        return candidateRepository.findByTenantId(defaultTenantId)
                .stream()
                .map(this::mapCandidateToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a specific candidate by ID.
     *
     * @param candidateId the candidate ID
     * @return candidate response DTO
     */
    public CandidateResponseDto getCandidateById(UUID candidateId) {
        log.debug("Fetching candidate with ID: {}", candidateId);
        
        Candidate candidateEntity = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Candidate not found: " + candidateId));
        
        return mapCandidateToResponseDto(candidateEntity);
    }

    /**
     * Deletes a candidate and all associated vectors.
     *
     * @param candidateId the candidate ID
     */
    public void deleteCandidate(UUID candidateId) {
        log.info("Deleting candidate with ID: {}", candidateId);
        
        Candidate candidateEntity = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Candidate not found: " + candidateId));
        
        // Delete Qdrant vectors
        if (candidateEntity.getQdrantPointIds() != null && !candidateEntity.getQdrantPointIds().isBlank()) {
            List<PointId> pointIds = Arrays.stream(candidateEntity.getQdrantPointIds().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> id(UUID.fromString(s)))
                    .collect(Collectors.toList());
            
            try {
                qdrantClient.deleteAsync(resumesCollection, pointIds).get();
                log.debug("Deleted {} Qdrant vectors for candidate: {}", pointIds.size(), candidateId);
            } catch (Exception exception) {
                log.error("Failed to delete Qdrant vectors for candidate {}: {}",
                        candidateId, exception.getMessage());
            }
        }
        
        // Delete resume metadata
        List<ResumeMetadata> resumeMetadataList = resumeMetadataRepository.findByCandidateId(candidateId);
        resumeMetadataRepository.deleteAll(resumeMetadataList);
        
        // Delete candidate
        candidateRepository.deleteById(candidateId);
        
        log.info("Candidate deleted successfully: {}", candidateId);
    }

    /**
     * Maps Candidate entity to CandidateResponseDto.
     *
     * @param candidateEntity the candidate entity
     * @return candidate response DTO
     */
    private CandidateResponseDto mapCandidateToResponseDto(Candidate candidateEntity) {
        return CandidateResponseDto.builder()
                .candidateId(candidateEntity.getId())
                .fullName(candidateEntity.getFullName())
                .emailAddress(candidateEntity.getEmailAddress())
                .phoneNumber(candidateEntity.getPhoneNumber())
                .source(candidateEntity.getSource())
                .createdAt(candidateEntity.getCreatedAt())
                .updatedAt(candidateEntity.getUpdatedAt())
                .build();
    }
}
