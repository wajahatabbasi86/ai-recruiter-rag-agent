package com.airecruiter.service;

import com.airecruiter.model.Job;
import com.airecruiter.model.MatchResult;
import com.airecruiter.model.ResumeMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Core matching engine implementing the hybrid scoring algorithm.
 *
 * Score breakdown:
 *   40% — vector cosine similarity
 *   40% — structured skill/framework/tool/DB matching
 *   10% — recency bonus (currently employed)
 *   10% — location bonus (location match)
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@Slf4j
@Service
public class MatchingEngineService {

    @Value("${app.matching.vector-weight:0.40}")
    private float vectorWeight;

    @Value("${app.matching.structured-weight:0.40}")
    private float structuredWeight;

    @Value("${app.matching.recency-weight:0.10}")
    private float recencyWeight;

    @Value("${app.matching.location-weight:0.10}")
    private float locationWeight;

    /**
     * Calculates a hybrid match score between a job and a candidate's resume metadata.
     *
     * @param job              the job entity
     * @param resumeMetadata   the candidate's resume metadata
     * @param vectorSimilarity pre-computed cosine similarity (0–100)
     * @return populated MatchResult (not yet persisted)
     */
    public MatchResult calculateMatchScore(Job job, ResumeMetadata resumeMetadata, Float vectorSimilarity) {
        log.debug("Calculating match score for job: {}, candidate: {}",
                job.getId(), resumeMetadata.getCandidateId());

        // 1. Vector component (already 0–100)
        float vectorComponent = (vectorSimilarity != null ? vectorSimilarity : 0f) * vectorWeight;

        // 2. Structured skill matching (0–100 internally, then weighted)
        StructuredMatchResult structuredResult = calculateStructuredScore(job, resumeMetadata);
        float structuredComponent = structuredResult.score * structuredWeight;

        // 3. Recency bonus
        float recencyBonus = calculateRecencyBonus(resumeMetadata) * recencyWeight * 100;

        // 4. Location bonus
        float locationBonus = calculateLocationBonus(job, resumeMetadata) * locationWeight * 100;

        float finalScore = vectorComponent + structuredComponent + recencyBonus + locationBonus;
        finalScore = Math.min(100f, Math.max(0f, finalScore)); // clamp 0–100

        log.debug("Scores — vector: {}, structured: {}, recency: {}, location: {}, final: {}",
                vectorComponent, structuredComponent, recencyBonus, locationBonus, finalScore);

        return MatchResult.builder()
                .jobId(job.getId())
                .candidateId(resumeMetadata.getCandidateId())
                .vectorSimilarityScore(vectorSimilarity)
                .structuredMatchingScore(structuredResult.score)
                .recencyBonusPoints(recencyBonus)
                .locationBonusPoints(locationBonus)
                .finalMatchScore(finalScore)
                .matchedSkills(String.join(", ", structuredResult.matchedSkills))
                .matchedFrameworks(String.join(", ", structuredResult.matchedFrameworks))
                .matchedTools(String.join(", ", structuredResult.matchedTools))
                .matchedDatabases(String.join(", ", structuredResult.matchedDatabases))
                .identifiedGaps(String.join(", ", structuredResult.gaps))
                .matchReasons(buildMatchReasons(structuredResult, vectorSimilarity, recencyBonus, locationBonus))
                .matchStatus("PENDING")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private StructuredMatchResult calculateStructuredScore(Job job, ResumeMetadata resume) {
        List<String> matchedSkills = matchTokens(job.getRequiredSkills(), resume.getTechnicalSkills());
        List<String> matchedFrameworks = matchTokens(job.getRequiredFrameworks(), resume.getFrameworks());
        List<String> matchedTools = matchTokens(job.getRequiredTools(), resume.getTools());
        List<String> matchedDatabases = matchTokens(job.getRequiredDatabases(), resume.getDatabases());
        List<String> gaps = findGaps(job.getRequiredSkills(), resume.getTechnicalSkills());

        // Experience score (0–1)
        float experienceScore = 0f;
        if (job.getMinimumExperienceYears() != null && resume.getTotalExperienceYears() != null) {
            int required = job.getMinimumExperienceYears();
            int actual = resume.getTotalExperienceYears();
            experienceScore = (required == 0) ? 1f : Math.min(1f, (float) actual / required);
        }

        // Weighted average across categories
        int totalRequired = countTokens(job.getRequiredSkills())
                + countTokens(job.getRequiredFrameworks())
                + countTokens(job.getRequiredTools())
                + countTokens(job.getRequiredDatabases());

        float skillMatchScore = 0f;
        if (totalRequired > 0) {
            int totalMatched = matchedSkills.size() + matchedFrameworks.size()
                    + matchedTools.size() + matchedDatabases.size();
            skillMatchScore = (float) totalMatched / totalRequired;
        }

        float structuredScore = ((skillMatchScore * 0.7f) + (experienceScore * 0.3f)) * 100f;

        return new StructuredMatchResult(structuredScore, matchedSkills, matchedFrameworks,
                matchedTools, matchedDatabases, gaps);
    }

    private float calculateRecencyBonus(ResumeMetadata resume) {
        if (resume.getCurrentEmploymentStatus() != null
                && resume.getCurrentEmploymentStatus().equalsIgnoreCase("Employed")) {
            return 1f;
        }
        if (resume.getLatestRoleEndDate() != null
                && resume.getLatestRoleEndDate().equalsIgnoreCase("Present")) {
            return 1f;
        }
        return 0f;
    }

    private float calculateLocationBonus(Job job, ResumeMetadata resume) {
        if (job.getRequiredLocation() == null || resume.getCurrentLocation() == null) {
            return 0f;
        }
        String jobLocation = job.getRequiredLocation().toLowerCase().trim();
        String candidateLocation = resume.getCurrentLocation().toLowerCase().trim();
        String candidateCountry = resume.getCountry() != null ? resume.getCountry().toLowerCase().trim() : "";

        if (candidateLocation.contains(jobLocation) || jobLocation.contains(candidateLocation)
                || candidateCountry.contains(jobLocation) || jobLocation.contains(candidateCountry)) {
            return 1f;
        }
        return 0f;
    }

    private List<String> matchTokens(String required, String actual) {
        if (required == null || actual == null) return List.of();
        Set<String> requiredSet = tokenize(required);
        Set<String> actualSet = tokenize(actual);
        return requiredSet.stream()
                .filter(actualSet::contains)
                .collect(Collectors.toList());
    }

    private List<String> findGaps(String required, String actual) {
        if (required == null) return List.of();
        Set<String> requiredSet = tokenize(required);
        Set<String> actualSet = actual != null ? tokenize(actual) : Set.of();
        return requiredSet.stream()
                .filter(r -> !actualSet.contains(r))
                .collect(Collectors.toList());
    }

    private Set<String> tokenize(String csv) {
        return Arrays.stream(csv.split("[,;]"))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private int countTokens(String csv) {
        if (csv == null || csv.isBlank()) return 0;
        return (int) Arrays.stream(csv.split("[,;]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .count();
    }

    private String buildMatchReasons(StructuredMatchResult result, Float vectorScore,
                                     float recencyBonus, float locationBonus) {
        List<String> reasons = new ArrayList<>();
        if (vectorScore != null && vectorScore > 60f) {
            reasons.add(String.format("High semantic similarity (%.1f%%)", vectorScore));
        }
        if (!result.matchedSkills.isEmpty()) {
            reasons.add("Matching skills: " + String.join(", ", result.matchedSkills));
        }
        if (!result.matchedFrameworks.isEmpty()) {
            reasons.add("Matching frameworks: " + String.join(", ", result.matchedFrameworks));
        }
        if (recencyBonus > 0) {
            reasons.add("Currently employed");
        }
        if (locationBonus > 0) {
            reasons.add("Location match");
        }
        return String.join("; ", reasons);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inner record for structured result
    // ─────────────────────────────────────────────────────────────────────────

    private record StructuredMatchResult(
            float score,
            List<String> matchedSkills,
            List<String> matchedFrameworks,
            List<String> matchedTools,
            List<String> matchedDatabases,
            List<String> gaps
    ) {}
}