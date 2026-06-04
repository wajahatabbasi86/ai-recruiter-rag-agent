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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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
     * @param jobId           the job ID
     * @param matchRequestDto the match request DTO
     */
    public void initiateMatching(UUID jobId, MatchRequestDto matchRequestDto) {
        log.info("Initiating matching process for job: {}", jobId);

        Job jobEntity = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

        if (Boolean.TRUE.equals(matchRequestDto.getIsAsync())) {
            performAsyncMatching(jobEntity, matchRequestDto);
        } else {
            performSyncMatching(jobEntity, matchRequestDto);
        }
    }

    /**
     * Performs matching asynchronously.
     *
     * @param jobEntity       the job entity
     * @param matchRequestDto the match request DTO
     */
    @Async
    public void performAsyncMatching(Job jobEntity, MatchRequestDto matchRequestDto) {
        log.info("Starting async matching for job: {}", jobEntity.getId());
        try {
            runMatching(jobEntity, matchRequestDto);
            log.info("Async matching completed for job: {}", jobEntity.getId());
        } catch (Exception exception) {
            log.error("Error during async matching for job {}: {}", jobEntity.getId(), exception.getMessage(), exception);
        }
    }

    /**
     * Performs matching synchronously.
     *
     * @param jobEntity       the job entity
     * @param matchRequestDto the match request DTO
     */
    public void performSyncMatching(Job jobEntity, MatchRequestDto matchRequestDto) {
        log.info("Starting sync matching for job: {}", jobEntity.getId());
        runMatching(jobEntity, matchRequestDto);
        log.info("Sync matching completed for job: {}", jobEntity.getId());
    }

    /**
     * Core matching logic shared by sync and async paths.
     * Clears previous results, scores all candidates, sorts by score, then assigns ranks.
     *
     * @param jobEntity       the job entity
     * @param matchRequestDto the match request DTO
     */
    @Transactional
    private void runMatching(Job jobEntity, MatchRequestDto matchRequestDto) {
        UUID jobId = jobEntity.getId();

        // FIX 1: Clear old match results before re-running to avoid duplicates
        matchResultRepository.deleteByJobId(jobId);
        log.debug("Cleared existing match results for job: {}", jobId);

        List<ResumeMetadata> resumeList = resumeMetadataRepository.findAll();
        if (resumeList.isEmpty()) {
            log.warn("No candidates found to match against job: {}", jobId);
            return;
        }

        // FIX 2: Embed the job description ONCE outside the candidate loop
        String jobEmbeddingText = buildJobEmbeddingText(jobEntity);
        float[] jobVector;
        try {
            jobVector = embeddingService.embedText(jobEmbeddingText);
        } catch (Exception exception) {
            log.error("Failed to embed job description for job {}: {}", jobId, exception.getMessage());
            return;
        }

        // Score all candidates
        List<MatchResult> matchResults = new ArrayList<>();
        for (ResumeMetadata resumeMetadata : resumeList) {
            try {
                String resumeEmbeddingText = buildResumeEmbeddingText(resumeMetadata);
                float[] resumeVector = embeddingService.embedText(resumeEmbeddingText);
                Float vectorSimilarity = calculateCosineSimilarity(jobVector, resumeVector) * 100f;

                MatchResult matchResult = matchingEngineService.calculateMatchScore(
                        jobEntity, resumeMetadata, vectorSimilarity);
                matchResults.add(matchResult);

            } catch (Exception exception) {
                log.error("Error matching resume for candidate {}: {}",
                        resumeMetadata.getCandidateId(), exception.getMessage());
            }
        }

        // FIX 3: Sort by finalMatchScore DESC before assigning ranks
        matchResults.sort(Comparator.comparingDouble(MatchResult::getFinalMatchScore).reversed());

        // Assign ranks and persist
        AtomicInteger rank = new AtomicInteger(1);
        for (MatchResult matchResult : matchResults) {
            matchResult.setCandidateRank(rank.getAndIncrement());
            matchResultRepository.save(matchResult);
        }

        log.info("Matching complete for job: {} — {} candidates ranked", jobId, matchResults.size());
    }

    /**
     * Retrieves shortlist for a job with optional filtering.
     */
    public List<MatchResultDto> getShortlist(UUID jobId, MatchRequestDto matchRequestDto) {
        log.debug("Fetching shortlist for job: {}", jobId);

        List<MatchResult> matchResults;

        if (matchRequestDto.getMinimumMatchScore() != null) {
            matchResults = matchResultRepository.findHighScoreMatchesForJob(
                    jobId, matchRequestDto.getMinimumMatchScore());
        } else {
            matchResults = matchResultRepository.findByJobIdOrderByCandidateRank(jobId);
        }

        return matchResults.stream()
                .map(this::mapMatchResultToDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves paginated shortlist for a job.
     */
    public Page<MatchResultDto> getShortlistPaginated(UUID jobId, Integer pageNumber, Integer pageSize) {
        log.debug("Fetching paginated shortlist for job: {}, page: {}, size: {}", jobId, pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return matchResultRepository.findByJobId(jobId, pageable).map(this::mapMatchResultToDto);
    }

    /**
     * Gets match result details for a specific candidate and job.
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
     */
    public void updateMatchStatus(UUID matchId, String newStatus) {
        log.info("Updating match status for match: {}, new status: {}", matchId, newStatus);
        MatchResult matchResult = matchResultRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found: " + matchId));
        matchResult.setMatchStatus(newStatus);
        matchResultRepository.save(matchResult);
        log.info("Match status updated successfully");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String buildJobEmbeddingText(Job jobEntity) {
        StringBuilder sb = new StringBuilder();
        if (jobEntity.getJobTitle() != null)       sb.append("Job Title: ").append(jobEntity.getJobTitle()).append(" ");
        if (jobEntity.getJobDescription() != null) sb.append("Description: ").append(jobEntity.getJobDescription()).append(" ");
        if (jobEntity.getRequiredSkills() != null)  sb.append("Skills: ").append(jobEntity.getRequiredSkills()).append(" ");
        if (jobEntity.getRequiredFrameworks() != null) sb.append("Frameworks: ").append(jobEntity.getRequiredFrameworks()).append(" ");
        if (jobEntity.getRequiredTools() != null)   sb.append("Tools: ").append(jobEntity.getRequiredTools()).append(" ");
        if (jobEntity.getRequiredDatabases() != null) sb.append("Databases: ").append(jobEntity.getRequiredDatabases()).append(" ");
        return sb.toString().trim();
    }

    private String buildResumeEmbeddingText(ResumeMetadata resumeMetadata) {
        StringBuilder sb = new StringBuilder();
        if (resumeMetadata.getTechnicalSkills() != null)   sb.append("Skills: ").append(resumeMetadata.getTechnicalSkills()).append(" ");
        if (resumeMetadata.getFrameworks() != null)        sb.append("Frameworks: ").append(resumeMetadata.getFrameworks()).append(" ");
        if (resumeMetadata.getTools() != null)             sb.append("Tools: ").append(resumeMetadata.getTools()).append(" ");
        if (resumeMetadata.getDatabases() != null)         sb.append("Databases: ").append(resumeMetadata.getDatabases()).append(" ");
        if (resumeMetadata.getJobTitlesHeld() != null)     sb.append("Job Titles: ").append(resumeMetadata.getJobTitlesHeld()).append(" ");
        if (resumeMetadata.getPreviousCompanies() != null) sb.append("Companies: ").append(resumeMetadata.getPreviousCompanies()).append(" ");
        if (resumeMetadata.getEducationLevel() != null)    sb.append("Education: ").append(resumeMetadata.getEducationLevel()).append(" ");
        return sb.toString().trim();
    }

    private Float calculateCosineSimilarity(float[] vector1, float[] vector2) {
        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("Vector dimensions do not match");
        }
        double dotProduct = 0.0, magnitude1 = 0.0, magnitude2 = 0.0;
        for (int i = 0; i < vector1.length; i++) {
            dotProduct  += vector1[i] * vector2[i];
            magnitude1  += vector1[i] * vector1[i];
            magnitude2  += vector2[i] * vector2[i];
        }
        magnitude1 = Math.sqrt(magnitude1);
        magnitude2 = Math.sqrt(magnitude2);
        if (magnitude1 == 0.0 || magnitude2 == 0.0) return 0.0f;
        return (float) (dotProduct / (magnitude1 * magnitude2));
    }

    /**
     * Maps MatchResult entity to MatchResultDto.
     * FIX 4: Populates currentLocation from ResumeMetadata instead of hardcoding null.
     */
    private MatchResultDto mapMatchResultToDto(MatchResult matchResult) {
        Optional<Candidate> candidateOpt = candidateRepository.findById(matchResult.getCandidateId());
        Candidate candidate = candidateOpt.orElse(null);

        // FIX 4: Fetch currentLocation from ResumeMetadata
        String currentLocation = resumeMetadataRepository
                .findFirstByCandidateId(matchResult.getCandidateId())
                .map(ResumeMetadata::getCurrentLocation)
                .orElse(null);

        return MatchResultDto.builder()
                .matchId(matchResult.getId())
                .jobId(matchResult.getJobId())
                .candidateId(matchResult.getCandidateId())
                .candidateName(candidate != null ? candidate.getFullName() : "Unknown")
                .emailAddress(candidate != null ? candidate.getEmailAddress() : null)
                .phoneNumber(candidate != null ? candidate.getPhoneNumber() : null)
                .currentLocation(currentLocation)
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
