package com.airecruiter.service;

import com.airecruiter.dto.MatchRequestDto;
import com.airecruiter.dto.MatchResultDto;
import com.airecruiter.model.Candidate;
import com.airecruiter.model.Job;
import com.airecruiter.model.MatchResult;
import com.airecruiter.model.ResumeMetadata;
import com.airecruiter.repository.CandidateRepository;
import com.airecruiter.repository.JobRepository;
import com.airecruiter.repository.MatchResultRepository;
import com.airecruiter.repository.ResumeMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MatchingService.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MatchingService Unit Tests")
class MatchingServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ResumeMetadataRepository resumeMetadataRepository;

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private MatchResultRepository matchResultRepository;

    @Mock
    private MatchingEngineService matchingEngineService;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private MatchingService matchingService;

    private UUID jobId;
    private UUID candidateId;
    private UUID matchId;
    private Job testJob;
    private Candidate testCandidate;
    private ResumeMetadata testResumeMetadata;
    private MatchResult testMatchResult;

    @BeforeEach
    void setUp() {
        jobId = UUID.randomUUID();
        candidateId = UUID.randomUUID();
        matchId = UUID.randomUUID();

        testJob = Job.builder()
                .id(jobId)
                .jobTitle("Senior Java Developer")
                .requiredSkills(new HashMap<String, String>() {{
                    put("java", "4+");
                }})
                .minimumExperienceYears(5)
                .build();

        testCandidate = Candidate.builder()
                .id(candidateId)
                .fullName("John Doe")
                .emailAddress("john.doe@example.com")
                .build();

        testResumeMetadata = ResumeMetadata.builder()
                .candidateId(candidateId)
                .technicalSkills(new HashMap<String, Integer>() {{
                    put("java", 5);
                }})
                .totalExperienceYears(8)
                .build();

        testMatchResult = MatchResult.builder()
                .id(matchId)
                .jobId(jobId)
                .candidateId(candidateId)
                .finalMatchScore(85.0f)
                .candidateRank(1)
                .matchStatus("shortlisted")
                .build();
    }

    @Test
    @DisplayName("Should get shortlist for a job")
    void testGetShortlist_Success() {
        List<MatchResult> matchResults = new ArrayList<>();
        matchResults.add(testMatchResult);

        when(matchResultRepository.findByJobIdOrderByCandidateRank(jobId))
                .thenReturn(matchResults);
        when(candidateRepository.findById(candidateId))
                .thenReturn(Optional.of(testCandidate));

        MatchRequestDto requestDto = MatchRequestDto.builder().build();

        List<MatchResultDto> shortlist = matchingService.getShortlist(jobId, requestDto);

        assertNotNull(shortlist);
        assertEquals(1, shortlist.size());
        assertEquals(testCandidate.getFullName(), shortlist.get(0).getCandidateName());
        verify(matchResultRepository, times(1)).findByJobIdOrderByCandidateRank(jobId);
    }

    @Test
    @DisplayName("Should get paginated shortlist")
    void testGetShortlistPaginated_Success() {
        List<MatchResult> matchResults = new ArrayList<>();
        matchResults.add(testMatchResult);
        Page<MatchResult> matchPage = new PageImpl<>(matchResults, PageRequest.of(0, 10), 1);

        when(matchResultRepository.findByJobId(eq(jobId), any()))
                .thenReturn(matchPage);
        when(candidateRepository.findById(candidateId))
                .thenReturn(Optional.of(testCandidate));

        Page<MatchResultDto> shortlist = matchingService.getShortlistPaginated(jobId, 0, 10);

        assertNotNull(shortlist);
        assertEquals(1, shortlist.getTotalElements());
        verify(matchResultRepository, times(1)).findByJobId(eq(jobId), any());
    }

    @Test
    @DisplayName("Should get match details for specific candidate")
    void testGetMatchDetails_Success() {
        List<MatchResult> matchResults = new ArrayList<>();
        matchResults.add(testMatchResult);

        when(matchResultRepository.findByJobIdAndCandidateId(jobId, candidateId))
                .thenReturn(matchResults);
        when(candidateRepository.findById(candidateId))
                .thenReturn(Optional.of(testCandidate));

        MatchResultDto details = matchingService.getMatchDetails(jobId, candidateId);

        assertNotNull(details);
        assertEquals(testMatchResult.getId(), details.getMatchId());
        verify(matchResultRepository, times(1)).findByJobIdAndCandidateId(jobId, candidateId);
    }

    @Test
    @DisplayName("Should update match status")
    void testUpdateMatchStatus_Success() {
        when(matchResultRepository.findById(matchId))
                .thenReturn(Optional.of(testMatchResult));
        when(matchResultRepository.save(any(MatchResult.class)))
                .thenReturn(testMatchResult);

        matchingService.updateMatchStatus(matchId, "rejected");

        verify(matchResultRepository, times(1)).findById(matchId);
        verify(matchResultRepository, times(1)).save(any(MatchResult.class));
    }

    @Test
    @DisplayName("Should trigger matching for job")
    void testInitiateMatching_Sync() {
        MatchRequestDto requestDto = MatchRequestDto.builder()
                .isAsync(false)
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(resumeMetadataRepository.findAll()).thenReturn(new ArrayList<>());

        matchingService.initiateMatching(jobId, requestDto);

        verify(jobRepository, times(1)).findById(jobId);
        verify(resumeMetadataRepository, times(1)).findAll();
    }
}
