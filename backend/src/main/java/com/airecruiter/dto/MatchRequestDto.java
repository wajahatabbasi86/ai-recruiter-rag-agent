package com.airecruiter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for match trigger requests.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchRequestDto {

    private Boolean isAsync;
    private Float minimumMatchScore;
    private Integer topK;
}
