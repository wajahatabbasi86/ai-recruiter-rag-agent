package com.airecruiter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for candidate profile responses.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateResponseDto {

    private UUID candidateId;
    private String fullName;
    private String emailAddress;
    private String phoneNumber;
    private String source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
