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
 * JPA entity for structured resume metadata extracted by LLM.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@Entity
@Table(name = "resume_metadata")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "technical_skills", columnDefinition = "TEXT")
    private String technicalSkills;

    @Column(name = "total_experience_years")
    private Integer totalExperienceYears;

    @Column(name = "previous_companies", columnDefinition = "TEXT")
    private String previousCompanies;

    @Column(name = "job_titles_held", columnDefinition = "TEXT")
    private String jobTitlesHeld;

    @Column(name = "education_level")
    private String educationLevel;

    @Column(name = "university_name")
    private String universityName;

    @Column(name = "fields_of_study", columnDefinition = "TEXT")
    private String fieldsOfStudy;

    @Column(name = "current_location")
    private String currentLocation;

    @Column(name = "country")
    private String country;

    @Column(name = "willing_to_relocate")
    private Boolean willingToRelocate;

    @Column(name = "tools", columnDefinition = "TEXT")
    private String tools;

    @Column(name = "frameworks", columnDefinition = "TEXT")
    private String frameworks;

    @Column(name = "databases", columnDefinition = "TEXT")
    private String databases;

    @Column(name = "latest_company_name")
    private String latestCompanyName;

    @Column(name = "latest_job_title")
    private String latestJobTitle;

    @Column(name = "latest_role_end_date")
    private String latestRoleEndDate;

    @Column(name = "current_employment_status")
    private String currentEmploymentStatus;

    @Column(name = "extracted_email")
    private String extractedEmail;

    @Column(name = "extracted_phone")
    private String extractedPhone;

    @Column(name = "linked_in_profile_url")
    private String linkedInProfileUrl;

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