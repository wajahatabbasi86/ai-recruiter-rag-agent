package com.airecruiter.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for job creation/update requests.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequestDto {

    @NotBlank(message = "Job title is required")
    @Size(max = 255, message = "Job title must not exceed 255 characters")
    private String jobTitle;

    @NotBlank(message = "Job description is required")
    private String jobDescription;

    @NotBlank(message = "Required skills are required")
    private String requiredSkills;

    @Min(value = 0, message = "Minimum experience years must be 0 or greater")
    private Integer minimumExperienceYears;

    @Min(value = 0, message = "Preferred experience years must be 0 or greater")
    private Integer preferredExperienceYears;

    private String requiredEducationLevel;
    private String requiredLocation;
    private String requiredTools;
    private String requiredFrameworks;
    private String requiredDatabases;
}
