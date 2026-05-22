package com.airecruiter.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "candidates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "source")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CandidateSource source = CandidateSource.RESUME;

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    /**
     * Comma-separated list of Qdrant point IDs — one per resume chunk.
     * A single resume produces multiple vectors (one per section),
     * so this field stores all of them for cleanup on delete.
     */
    @Column(name = "qdrant_point_ids", columnDefinition = "TEXT")
    private String qdrantPointIds;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum CandidateSource {
        RESUME, LINKEDIN, REFERRAL, MANUAL
    }
}
