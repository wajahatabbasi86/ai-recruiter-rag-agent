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
 * JPA entity for a job description.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@Entity
@Table(name = "jobs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "job_title", nullable = false)
    private String jobTitle;

    @Column(name = "job_description", columnDefinition = "TEXT")
    private String jobDescription;

    @Column(name = "required_skills", columnDefinition = "TEXT")
    private String requiredSkills;

    @Column(name = "minimum_experience_years")
    private Integer minimumExperienceYears;

    @Column(name = "preferred_experience_years")
    private Integer preferredExperienceYears;

    @Column(name = "required_education_level")
    private String requiredEducationLevel;

    @Column(name = "required_location")
    private String requiredLocation;

    @Column(name = "required_tools", columnDefinition = "TEXT")
    private String requiredTools;

    @Column(name = "required_frameworks", columnDefinition = "TEXT")
    private String requiredFrameworks;

    @Column(name = "required_databases", columnDefinition = "TEXT")
    private String requiredDatabases;

    @Column(name = "original_file_path")
    private String originalFilePath;

    @Column(name = "raw_extracted_text", columnDefinition = "TEXT")
    private String rawExtractedText;

    @Column(name = "job_status")
    private String jobStatus;

    @Column(name = "qdrant_point_ids", columnDefinition = "TEXT")
    private String qdrantPointIds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}