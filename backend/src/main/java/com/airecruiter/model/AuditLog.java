package com.airecruiter.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit log entry for every AI query.
 *
 * Required for EU AI Act compliance — all AI-assisted hiring decisions
 * must be traceable. This table is your evidence trail.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "query_text", columnDefinition = "TEXT", nullable = false)
    private String queryText;

    @Column(name = "matched_ids", columnDefinition = "TEXT")
    private String matchedIds;     // Comma-separated candidate IDs

    @Column(name = "bias_detected")
    @Builder.Default
    private Boolean biasDetected = false;

    @Column(name = "model_used")
    private String modelUsed;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
