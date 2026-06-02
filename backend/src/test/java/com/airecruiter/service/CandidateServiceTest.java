package com.airecruiter.service;

import com.airecruiter.dto.CandidateResponseDto;
import com.airecruiter.dto.ResumeMetadataDto;
import com.airecruiter.model.Candidate;
import com.airecruiter.model.ResumeMetadata;
import com.airecruiter.repository.CandidateRepository;
import com.airecruiter.repository.ResumeMetadataRepository;
import io.qdrant.client.QdrantClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CandidateService.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CandidateService Unit Tests")
class CandidateServiceTest {

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private ResumeMetadataRepository resumeMetadataRepository;

    @Mock
    private ResumeExtractionService resumeExtractionService;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private DuplicateDetectionService duplicateDetectionService;

    @Mock
    private QdrantClient qdrantClient;

    @InjectMocks
    private CandidateService candidateService;

    private UUID defaultTenantId;
    private UUID candidateId;
    private UUID resumeMetadataId;
    private Candidate testCandidate;
    private ResumeMetadata testResumeMetadata;
    private MultipartFile mockFile;

    @BeforeEach
    void setUp() {
        defaultTenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        candidateId = UUID.randomUUID();
        resumeMetadataId = UUID.randomUUID();

        ReflectionTestUtils.setField(candidateService, "defaultTenantId", defaultTenantId);
        ReflectionTestUtils.setField(candidateService, "resumesCollection", "resume-chunks");

        testCandidate = Candidate.builder()
                .id(candidateId)
                .tenantId(defaultTenantId)
                .fullName("John Doe")
                .emailAddress("john.doe@example.com")
                .phoneNumber("+1-555-0123")
                .source("RESUME")
                .build();

        testResumeMetadata = ResumeMetadata.builder()
                .id(resumeMetadataId)
                .candidateId(candidateId)
                .technicalSkills(new HashMap<String, Integer>() {{
                    put("java", 5);
                    put("spring", 3);
                }})
                .totalExperienceYears(8)
                .educationLevel("Bachelor's")
                .currentLocation("San Francisco, USA")
                .build();
    }

    @Test
    @DisplayName("Should ingest resume and create candidate profile")
    void testIngestResume_Success() throws IOException {
        mockFile = org.mockito.Mockito.mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("john_doe_resume.pdf");

        ResumeMetadataDto metadataDto = ResumeMetadataDto.builder()
                .technicalSkills(testResumeMetadata.getTechnicalSkills())
                .totalExperienceYears(8)
                .educationLevel("Bachelor's")
                .currentLocation("San Francisco, USA")
                .build();

        when(resumeExtractionService.extractTextFromFile(mockFile))
                .thenReturn("Resume text content");
        when(resumeExtractionService.extractStructuredMetadata(anyString()))
                .thenReturn(metadataDto);
        when(candidateRepository.save(any(Candidate.class))).thenReturn(testCandidate);
        when(resumeMetadataRepository.save(any(ResumeMetadata.class))).thenReturn(testResumeMetadata);

        CandidateResponseDto response = candidateService.ingestResume(mockFile, "John Doe");

        assertNotNull(response);
        assertEquals(testCandidate.getId(), response.getCandidateId());
        assertEquals(testCandidate.getFullName(), response.getFullName());
        verify(candidateRepository, times(2)).save(any(Candidate.class));
        verify(resumeMetadataRepository, times(1)).save(any(ResumeMetadata.class));
    }

    @Test
    @DisplayName("Should retrieve all candidates")
    void testGetAllCandidates_Success() {
        List<Candidate> candidates = new ArrayList<>();
        candidates.add(testCandidate);

        when(candidateRepository.findByTenantId(defaultTenantId)).thenReturn(candidates);

        List<CandidateResponseDto> response = candidateService.getAllCandidates();

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(testCandidate.getFullName(), response.get(0).getFullName());
        verify(candidateRepository, times(1)).findByTenantId(defaultTenantId);
    }

    @Test
    @DisplayName("Should retrieve candidate by ID")
    void testGetCandidateById_Success() {
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(testCandidate));

        CandidateResponseDto response = candidateService.getCandidateById(candidateId);

        assertNotNull(response);
        assertEquals(testCandidate.getId(), response.getCandidateId());
        assertEquals(testCandidate.getFullName(), response.getFullName());
        verify(candidateRepository, times(1)).findById(candidateId);
    }

    @Test
    @DisplayName("Should throw exception when candidate not found")
    void testGetCandidateById_NotFound() {
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> candidateService.getCandidateById(candidateId));
        verify(candidateRepository, times(1)).findById(candidateId);
    }

    @Test
    @DisplayName("Should delete candidate successfully")
    void testDeleteCandidate_Success() {
        List<ResumeMetadata> resumeMetadataList = new ArrayList<>();
        resumeMetadataList.add(testResumeMetadata);

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(testCandidate));
        when(resumeMetadataRepository.findByCandidateId(candidateId)).thenReturn(resumeMetadataList);
        doReturn(null).when(qdrantClient).deleteAsync(anyString(), any());

        candidateService.deleteCandidate(candidateId);

        verify(candidateRepository, times(1)).findById(candidateId);
        verify(resumeMetadataRepository, times(1)).deleteAll(resumeMetadataList);
        verify(candidateRepository, times(1)).deleteById(candidateId);
    }
}
