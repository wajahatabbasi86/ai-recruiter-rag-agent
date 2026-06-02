package com.airecruiter.service;

import com.airecruiter.dto.MatchRequestDto;
import com.airecruiter.dto.MatchResultDto;
import com.airecruiter.model.Candidate;
import com.airecruiter.model.Job;
import com.airecruiter.model.MatchResult;
import com.airecruiter.model.ResumeMetadata;
import com.airecruiter.repository.CandidateRepository;
import com.airecruiter.repository.JobRepository;
import com.airecruiter.repository.MatchResultRepository;
import com.airecruiter.repository.ResumeMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for resume-to-job matching.
 * Orchestrates matching process and manages match results.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService {

    private final JobRepository jobRepository;
    private final ResumeMetadataRepository resumeMetadataRepository;
    private final CandidateRepository candidateRepository;
    private final MatchResultRepository matchResultRepository;
    private final MatchingEngineService matchingEngineService;
    private final EmbeddingService embeddingService;

    /**
     * Initiates matching process for a job against all candidates.
     *
     * @param jobId the job ID
     * @param matchRequestDto the match request DTO
     */
    public void initiateMatching(UUID jobId, MatchRequestDto matchRequestDto) {
        log.info("Initiating matching process for job: {}", jobId);
        
        if (Boolean.TRUE.equals(matchRequestDto.getIsAsync())) {
            performAsyncMatching(jobId, matchRequestDto);
        } else {
            performSyncMatching(jobId, matchRequestDto);
        }
    }

    /**
     * Performs matching asynchronously.
     *
     * @param jobId the job ID
     * @param matchRequestDto the match request DTO
     */
    @Async
    public void performAsyncMatching(UUID jobId, MatchRequestDto matchRequestDto) {
        log.info("Starting async matching for job: {}", jobId);
        
        try {
            Job jobEntity = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
            
            List<ResumeMetadata> resumeList = resumeMetadataRepository.findAll();
            
            Integer rank = 1;
            for (ResumeMetadata resumeMetadata : resumeList) {
                try {
                    // Embed job description
                    String jobEmbeddingText = buildJobEmbeddingText(jobEntity);
                    float[] jobVector = embeddingService.embedText(jobEmbeddingText);
                    
                    // Embed resume
                    String resumeEmbeddingText = buildResumeEmbeddingText(resumeMetadata);
                    float[] resumeVector = embeddingService.embedText(resumeEmbeddingText);
                    
                    // Calculate vector similarity
                    Float vectorSimilarity = calculateCosineSimilarity(jobVector, resumeVector) * 100;
                    
                    // Calculate match score
                    MatchResult matchResult = matchingEngineService.calculateMatchScore(
                            jobEntity,
                            resumeMetadata,
                            vectorSimilarity
                    );
                    
                    matchResult.setCandidateRank(rank);
                    matchResultRepository.save(matchResult);
                    
                    rank++;
                    
                } catch (Exception exception) {
                    log.error("Error matching resume for candidate {}: {}",
                            resumeMetadata.getCandidateId(), exception.getMessage());
                }
            }
            
            log.info("Async matching completed for job: {}", jobId);
            
        } catch (Exception exception) {
            log.error("Error during async matching for job {}: {}", jobId, exception.getMessage(), exception);
        }
    }

    /**
     * Performs matching synchronously.
     *
     * @param jobId the job ID
     * @param matchRequestDto the match request DTO
     */
    public void performSyncMatching(UUID jobId, MatchRequestDto matchRequestDto) {
        log.info("Starting sync matching for job: {}", jobId);
        
        Job jobEntity = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        
        List<ResumeMetadata> resumeList = resumeMetadataRepository.findAll();
        
        Integer rank = 1;
        for (ResumeMetadata resumeMetadata : resumeList) {
            try {
                // Embed job description
                String jobEmbeddingText = buildJobEmbeddingText(jobEntity);
                float[] jobVector = embeddingService.embedText(jobEmbeddingText);
                
                // Embed resume
                String resumeEmbeddingText = buildResumeEmbeddingText(resumeMetadata);
                float[] resumeVector = embeddingService.embedText(resumeEmbeddingText);
                
                // Calculate vector similarity
                Float vectorSimilarity = calculateCosineSimilarity(jobVector, resumeVector) * 100;
                
                // Calculate match score
                MatchResult matchResult = matchingEngineService.calculateMatchScore(
                        jobEntity,
                        resumeMetadata,
                        vectorSimilarity
                );
                
                matchResult.setCandidateRank(rank);
                matchResultRepository.save(matchResult);
                
                rank++;
                
            } catch (Exception exception) {
                log.error("Error matching resume for candidate {}: {}",
                        resumeMetadata.getCandidateId(), exception.getMessage());
            }
        }
        
        log.info("Sync matching completed for job: {}", jobId);
    }

    /**
     * Retrieves shortlist for a job with optional filtering.
     *
     * @param jobId the job ID
     * @param matchRequestDto the match request DTO
     * @return list of match result DTOs
     */
    public List<MatchResultDto> getShortlist(UUID jobId, MatchRequestDto matchRequestDto) {
        log.debug("Fetching shortlist for job: {}", jobId);
        
        List<MatchResult> matchResults;
        
        if (matchRequestDto.getMinimumMatchScore() != null) {
            matchResults = matchResultRepository.findHighScoreMatchesForJob(
                    jobId,
                    matchRequestDto.getMinimumMatchScore()
            );
        } else {
            matchResults = matchResultRepository.findByJobIdOrderByCandidateRank(jobId);
        }
        
        return matchResults.stream()
                .map(this::mapMatchResultToDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves paginated shortlist for a job.
     *
     * @param jobId the job ID
     * @param pageNumber the page number (0-indexed)
     * @param pageSize the page size
     * @return page of match result DTOs
     */
    public Page<MatchResultDto> getShortlistPaginated(UUID jobId, Integer pageNumber, Integer pageSize) {
        log.debug("Fetching paginated shortlist for job: {}, page: {}, size: {}",
                jobId, pageNumber, pageSize);
        
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        
        return matchResultRepository.findByJobId(jobId, pageable)
                .map(this::mapMatchResultToDto);
    }

    /**
     * Gets match result details for a specific candidate and job.
     *
     * @param jobId the job ID
     * @param candidateId the candidate ID
     * @return match result DTO
     */
    public MatchResultDto getMatchDetails(UUID jobId, UUID candidateId) {
        log.debug("Fetching match details for job: {}, candidate: {}", jobId, candidateId);
        
        List<MatchResult> matchResults = matchResultRepository.findByJobIdAndCandidateId(jobId, candidateId);
        
        if (matchResults.isEmpty()) {
            throw new RuntimeException("Match not found for job: " + jobId + ", candidate: " + candidateId);
        }
        
        return mapMatchResultToDto(matchResults.get(0));
    }

    /**
     * Updates match status.
     *
     * @param matchId the match ID
     * @param newStatus the new status
     */
    public void updateMatchStatus(UUID matchId, String newStatus) {
        log.info("Updating match status for match: {}, new status: {}", matchId, newStatus);
        
        MatchResult matchResult = matchResultRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found: " + matchId));
        
        matchResult.setMatchStatus(newStatus);
        matchResultRepository.save(matchResult);
        
        log.info("Match status updated successfully");
    }

    /**
     * Builds embedding text for job description.
     *
     * @param jobEntity the job entity
     * @return embedding text
     */
    private String buildJobEmbeddingText(Job jobEntity) {
        StringBuilder embeddingText = new StringBuilder();
        
        if (jobEntity.getJobTitle() != null) {
            embeddingText.append("Job Title: ").append(jobEntity.getJobTitle()).append(" ");
        }
        
        if (jobEntity.getJobDescription() != null) {
            embeddingText.append("Description: ").append(jobEntity.getJobDescription()).append(" ");
        }
        
        if (jobEntity.getRequiredSkills() != null) {
            embeddingText.append("Skills: ").append(jobEntity.getRequiredSkills()).append(" ");
        }
        
        return embeddingText.toString();
    }

    /**
     * Builds embedding text for resume.
     *
     * @param resumeMetadata the resume metadata
     * @return embedding text
     */
    private String buildResumeEmbeddingText(ResumeMetadata resumeMetadata) {
        StringBuilder embeddingText = new StringBuilder();
        
        if (resumeMetadata.getTechnicalSkills() != null) {
            embeddingText.append("Skills: ").append(resumeMetadata.getTechnicalSkills()).append(" ");
        }
        
        if (resumeMetadata.getPreviousCompanies() != null) {
            embeddingText.append("Companies: ").append(resumeMetadata.getPreviousCompanies()).append(" ");
        }
        
        if (resumeMetadata.getEducationLevel() != null) {
            embeddingText.append("Education: ").append(resumeMetadata.getEducationLevel()).append(" ");
        }
        
        return embeddingText.toString();
    }

    /**
     * Calculates cosine similarity between two vectors.
     *
     * @param vector1 the first vector
     * @param vector2 the second vector
     * @return cosine similarity (0-1)
     */
    private Float calculateCosineSimilarity(float[] vector1, float[] vector2) {
        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("Vector dimensions do not match");
        }
        
        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;
        
        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            magnitude1 += vector1[i] * vector1[i];
            magnitude2 += vector2[i] * vector2[i];
        }
        
        magnitude1 = Math.sqrt(magnitude1);
        magnitude2 = Math.sqrt(magnitude2);
        
        if (magnitude1 == 0.0 || magnitude2 == 0.0) {
            return 0.0f;
        }
        
        return (float) (dotProduct / (magnitude1 * magnitude2));
    }

    /**
     * Maps MatchResult entity to MatchResultDto.
     *
     * @param matchResult the match result entity
     * @return match result DTO
     */
    private MatchResultDto mapMatchResultToDto(MatchResult matchResult) {
        Optional<Candidate> candidateOpt = candidateRepository.findById(matchResult.getCandidateId());
        Candidate candidate = candidateOpt.orElse(null);
        
        return MatchResultDto.builder()
                .matchId(matchResult.getId())
                .jobId(matchResult.getJobId())
                .candidateId(matchResult.getCandidateId())
                .candidateName(candidate != null ? candidate.getFullName() : "Unknown")
                .emailAddress(candidate != null ? candidate.getEmailAddress() : null)
                .phoneNumber(candidate != null ? candidate.getPhoneNumber() : null)
                .currentLocation(null)
                .vectorSimilarityScore(matchResult.getVectorSimilarityScore())
                .structuredMatchingScore(matchResult.getStructuredMatchingScore())
                .recencyBonusPoints(matchResult.getRecencyBonusPoints())
                .locationBonusPoints(matchResult.getLocationBonusPoints())
                .finalMatchScore(matchResult.getFinalMatchScore())
                .candidateRank(matchResult.getCandidateRank())
                .matchedSkills(matchResult.getMatchedSkills())
                .matchedFrameworks(matchResult.getMatchedFrameworks())
                .matchedTools(matchResult.getMatchedTools())
                .matchedDatabases(matchResult.getMatchedDatabases())
                .identifiedGaps(matchResult.getIdentifiedGaps())
                .matchReasons(matchResult.getMatchReasons())
                .matchStatus(matchResult.getMatchStatus())
                .createdAt(matchResult.getCreatedAt())
                .build();
    }
}
