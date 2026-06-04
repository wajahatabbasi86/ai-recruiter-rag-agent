package com.airecruiter.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for storing match results between a job and a candidate.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@Entity
@Table(name = "match_results")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "vector_similarity_score")
    private Float vectorSimilarityScore;

    @Column(name = "structured_matching_score")
    private Float structuredMatchingScore;

    @Column(name = "recency_bonus_points")
    private Float recencyBonusPoints;

    @Column(name = "location_bonus_points")
    private Float locationBonusPoints;

    @Column(name = "final_match_score")
    private Float finalMatchScore;

    @Column(name = "candidate_rank")
    private Integer candidateRank;

    @Column(name = "matched_skills", columnDefinition = "TEXT")
    private String matchedSkills;

    @Column(name = "matched_frameworks", columnDefinition = "TEXT")
    private String matchedFrameworks;

    @Column(name = "matched_tools", columnDefinition = "TEXT")
    private String matchedTools;

    @Column(name = "matched_databases", columnDefinition = "TEXT")
    private String matchedDatabases;

    @Column(name = "identified_gaps", columnDefinition = "TEXT")
    private String identifiedGaps;

    @Column(name = "match_reasons", columnDefinition = "TEXT")
    private String matchReasons;

    @Column(name = "match_status")
    private String matchStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (matchStatus == null) {
            matchStatus = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}