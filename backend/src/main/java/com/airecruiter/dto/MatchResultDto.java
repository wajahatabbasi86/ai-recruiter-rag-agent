package com.airecruiter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for match result responses.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResultDto {

    private UUID matchId;
    private UUID jobId;
    private UUID candidateId;
    private String candidateName;
    private String emailAddress;
    private String phoneNumber;
    private String currentLocation;
    private Float vectorSimilarityScore;
    private Float structuredMatchingScore;
    private Float recencyBonusPoints;
    private Float locationBonusPoints;
    private Float finalMatchScore;
    private Integer candidateRank;
    private String matchedSkills;
    private String matchedFrameworks;
    private String matchedTools;
    private String matchedDatabases;
    private String identifiedGaps;
    private String matchReasons;
    private String matchStatus;
    private LocalDateTime createdAt;
}
