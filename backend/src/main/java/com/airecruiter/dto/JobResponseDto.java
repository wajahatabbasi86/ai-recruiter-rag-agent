package com.airecruiter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for job description responses.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponseDto {

    private UUID jobId;
    private String jobTitle;
    private String jobDescription;
    private String requiredSkills;
    private Integer minimumExperienceYears;
    private Integer preferredExperienceYears;
    private String requiredEducationLevel;
    private String requiredLocation;
    private String requiredTools;
    private String requiredFrameworks;
    private String requiredDatabases;
    private String jobStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String message;
}
