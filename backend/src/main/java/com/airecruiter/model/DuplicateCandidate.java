package com.airecruiter.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for tracking detected duplicate candidates.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@Entity
@Table(name = "duplicate_candidates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "duplicate_candidate_id", nullable = false)
    private UUID duplicateCandidateId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "similarity_score")
    private Double similarityScore;

    @Column(name = "detection_reason")
    private String detectionReason;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING_REVIEW";
        }
    }
}