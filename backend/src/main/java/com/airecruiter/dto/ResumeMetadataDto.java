package com.airecruiter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for structured resume metadata extracted by LLM.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeMetadataDto {

    private String technicalSkills;
    private Integer totalExperienceYears;
    private String previousCompanies;
    private String jobTitlesHeld;
    private String educationLevel;
    private String universityName;
    private String fieldsOfStudy;
    private String currentLocation;
    private String country;
    private Boolean willingToRelocate;
    private String tools;
    private String frameworks;
    private String databases;
    private String latestCompanyName;
    private String latestJobTitle;
    private String latestRoleEndDate;
    private String currentEmploymentStatus;
    private String extractedEmail;
    private String extractedPhone;
    private String linkedInProfileUrl;
}
