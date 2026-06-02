package com.airecruiter.service;

import com.airecruiter.model.Job;
import com.airecruiter.model.MatchResult;
import com.airecruiter.model.ResumeMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for MatchingEngineService.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MatchingEngineService Unit Tests")
class MatchingEngineServiceTest {

    @InjectMocks
    private MatchingEngineService matchingEngineService;

    private Job jobEntity;
    private ResumeMetadata resumeMetadata;
    private UUID jobId;
    private UUID candidateId;
    private UUID resumeMetadataId;

    @BeforeEach
    void setUp() {
        jobId = UUID.randomUUID();
        candidateId = UUID.randomUUID();
        resumeMetadataId = UUID.randomUUID();

        jobEntity = Job.builder()
                .id(jobId)
                .jobTitle("Senior Java Developer")
                .requiredSkills(new HashMap<String, String>() {{
                    put("java", "4+");
                    put("spring", "3+");
                }})
                .minimumExperienceYears(5)
                .requiredEducationLevel("Bachelor's")
                .requiredLocation("San Francisco, USA")
                .build();

        resumeMetadata = ResumeMetadata.builder()
                .id(resumeMetadataId)
                .candidateId(candidateId)
                .technicalSkills(new HashMap<String, Integer>() {{
                    put("java", 5);
                    put("spring", 3);
                }})
                .totalExperienceYears(8)
                .educationLevel("Bachelor's")
                .currentLocation("San Francisco, USA")
                .latestRoleEndDate(null) // Currently employed
                .build();
    }

    @Test
    @DisplayName("Should calculate match score correctly")
    void testCalculateMatchScore_Success() {
        Float vectorScore = 85.0f;

        MatchResult matchResult = matchingEngineService.calculateMatchScore(
                jobEntity,
                resumeMetadata,
                vectorScore
        );

        assertNotNull(matchResult);
        assertNotNull(matchResult.getFinalMatchScore());
        assertTrue(matchResult.getFinalMatchScore() >= 0);
        assertTrue(matchResult.getFinalMatchScore() <= 100);
        assertEquals(jobId, matchResult.getJobId());
        assertEquals(candidateId, matchResult.getCandidateId());
    }

    @Test
    @DisplayName("Should identify skill gaps correctly")
    void testIdentifyGaps_MissingSkills() {
        // Resume missing 'spring' skill
        ResumeMetadata resumeWithGaps = ResumeMetadata.builder()
                .id(resumeMetadataId)
                .candidateId(candidateId)
                .technicalSkills(new HashMap<String, Integer>() {{
                    put("java", 4);
                }})
                .totalExperienceYears(3)
                .build();

        Float vectorScore = 60.0f;

        MatchResult matchResult = matchingEngineService.calculateMatchScore(
                jobEntity,
                resumeWithGaps,
                vectorScore
        );

        assertNotNull(matchResult);
        assertNotNull(matchResult.getIdentifiedGaps());
        assertTrue(matchResult.getIdentifiedGaps().size() > 0);
    }

    @Test
    @DisplayName("Should apply recency bonus for currently employed")
    void testRecencyBonus_CurrentlyEmployed() {
        Float vectorScore = 80.0f;

        MatchResult matchResult = matchingEngineService.calculateMatchScore(
                jobEntity,
                resumeMetadata,
                vectorScore
        );

        assertNotNull(matchResult);
        assertEquals(10.0f, matchResult.getRecencyBonusPoints());
    }

    @Test
    @DisplayName("Should apply recency penalty for employment gap")
    void testRecencyBonus_EmploymentGap() {
        ResumeMetadata resumeWithGap = ResumeMetadata.builder()
                .id(resumeMetadataId)
                .candidateId(candidateId)
                .technicalSkills(new HashMap<String, Integer>() {{
                    put("java", 5);
                }})
                .totalExperienceYears(8)
                .latestRoleEndDate(LocalDate.now().minusDays(180)) // 6 months gap
                .build();

        Float vectorScore = 80.0f;

        MatchResult matchResult = matchingEngineService.calculateMatchScore(
                jobEntity,
                resumeWithGap,
                vectorScore
        );

        assertNotNull(matchResult);
        assertTrue(matchResult.getRecencyBonusPoints() < 10.0f);
    }

    @Test
    @DisplayName("Should apply location bonus for exact match")
    void testLocationBonus_ExactMatch() {
        Float vectorScore = 80.0f;

        MatchResult matchResult = matchingEngineService.calculateMatchScore(
                jobEntity,
                resumeMetadata,
                vectorScore
        );

        assertNotNull(matchResult);
        assertEquals(10.0f, matchResult.getLocationBonusPoints());
    }

    @Test
    @DisplayName("Should apply location bonus for willing to relocate")
    void testLocationBonus_WillingToRelocate() {
        ResumeMetadata resumeWillingToRelocate = ResumeMetadata.builder()
                .id(resumeMetadataId)
                .candidateId(candidateId)
                .technicalSkills(resumeMetadata.getTechnicalSkills())
                .totalExperienceYears(8)
                .currentLocation("New York, USA")
                .willingToRelocate(true)
                .build();

        Float vectorScore = 80.0f;

        MatchResult matchResult = matchingEngineService.calculateMatchScore(
                jobEntity,
                resumeWillingToRelocate,
                vectorScore
        );

        assertNotNull(matchResult);
        assertTrue(matchResult.getLocationBonusPoints() > 0);
    }
}
