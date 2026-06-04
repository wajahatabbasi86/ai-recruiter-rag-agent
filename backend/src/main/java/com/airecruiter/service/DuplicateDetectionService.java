package com.airecruiter.service;

import com.airecruiter.model.Candidate;
import com.airecruiter.model.DuplicateCandidate;
import com.airecruiter.repository.CandidateRepository;
import com.airecruiter.repository.DuplicateCandidateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for detecting duplicate candidate profiles.
 * Uses email matching, phone matching, and Levenshtein distance on names.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DuplicateDetectionService {

    private static final double NAME_SIMILARITY_THRESHOLD = 0.85;

    private final CandidateRepository candidateRepository;
    private final DuplicateCandidateRepository duplicateCandidateRepository;

    /**
     * Detects potential duplicate candidates for a given candidate.
     *
     * @param candidateId the candidate ID to check
     * @param tenantId    the tenant ID
     * @return list of detected duplicate records
     */
    public List<DuplicateCandidate> detectDuplicates(UUID candidateId, UUID tenantId) {
        log.debug("Running duplicate detection for candidate: {}", candidateId);

        Candidate candidate = candidateRepository.findById(candidateId).orElse(null);
        if (candidate == null) {
            log.warn("Candidate not found for duplicate detection: {}", candidateId);
            return List.of();
        }

        List<Candidate> existingCandidates = candidateRepository.findByTenantId(tenantId);
        List<DuplicateCandidate> duplicates = new ArrayList<>();

        for (Candidate existing : existingCandidates) {
            if (existing.getId().equals(candidateId)) continue;

            String reason = detectDuplicateReason(candidate, existing);
            if (reason != null) {
                double similarity = calculateNameSimilarity(
                        candidate.getFullName(), existing.getFullName());

                DuplicateCandidate duplicate = DuplicateCandidate.builder()
                        .candidateId(candidateId)
                        .duplicateCandidateId(existing.getId())
                        .tenantId(tenantId)
                        .similarityScore(similarity)
                        .detectionReason(reason)
                        .status("PENDING_REVIEW")
                        .build();

                duplicates.add(duplicateCandidateRepository.save(duplicate));
                log.info("Duplicate detected: candidate {} matches {} — reason: {}",
                        candidateId, existing.getId(), reason);
            }
        }

        return duplicates;
    }

    /**
     * Determines if two candidates are likely duplicates and returns the reason.
     *
     * @param candidate the new candidate
     * @param existing  an existing candidate
     * @return reason string or null if not a duplicate
     */
    private String detectDuplicateReason(Candidate candidate, Candidate existing) {
        // Email match
        if (candidate.getEmailAddress() != null && existing.getEmailAddress() != null
                && candidate.getEmailAddress().equalsIgnoreCase(existing.getEmailAddress())) {
            return "EMAIL_MATCH";
        }

        // Phone match
        if (candidate.getPhoneNumber() != null && existing.getPhoneNumber() != null
                && normalizePhone(candidate.getPhoneNumber())
                .equals(normalizePhone(existing.getPhoneNumber()))) {
            return "PHONE_MATCH";
        }

        // Name similarity (Levenshtein)
        if (candidate.getFullName() != null && existing.getFullName() != null) {
            double similarity = calculateNameSimilarity(candidate.getFullName(), existing.getFullName());
            if (similarity >= NAME_SIMILARITY_THRESHOLD) {
                return String.format("NAME_SIMILARITY_%.2f", similarity);
            }
        }

        return null;
    }

    /**
     * Calculates normalised Levenshtein similarity between two strings.
     *
     * @param name1 first name
     * @param name2 second name
     * @return similarity score in [0, 1]
     */
    private double calculateNameSimilarity(String name1, String name2) {
        if (name1 == null || name2 == null) return 0.0;
        String n1 = name1.toLowerCase().trim();
        String n2 = name2.toLowerCase().trim();
        int distance = StringUtils.getLevenshteinDistance(n1, n2);
        int maxLen = Math.max(n1.length(), n2.length());
        return maxLen == 0 ? 1.0 : 1.0 - ((double) distance / maxLen);
    }

    /**
     * Normalises a phone number by stripping non-digit characters.
     *
     * @param phone the phone string
     * @return digits-only string
     */
    private String normalizePhone(String phone) {
        return phone.replaceAll("[^0-9]", "");
    }
}