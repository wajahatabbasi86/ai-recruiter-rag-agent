package com.airecruiter.dto;

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
}
