package com.airecruiter.service;

import com.airecruiter.model.Candidate;
import com.airecruiter.model.DuplicateCandidate;
import com.airecruiter.model.ResumeMetadata;
import com.airecruiter.repository.CandidateRepository;
import com.airecruiter.repository.DuplicateCandidateRepository;
import com.airecruiter.repository.ResumeMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DuplicateDetectionService.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DuplicateDetectionService Unit Tests")
class DuplicateDetectionServiceTest {

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private ResumeMetadataRepository resumeMetadataRepository;

    @Mock
    private DuplicateCandidateRepository duplicateCandidateRepository;

    @InjectMocks
    private DuplicateDetectionService duplicateDetectionService;

    private UUID tenantId;
    private UUID candidate1Id;
    private UUID candidate2Id;
    private Candidate candidate1;
    private Candidate candidate2;

    @BeforeEach
    void setUp() {
        tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        candidate1Id = UUID.randomUUID();
        candidate2Id = UUID.randomUUID();

        candidate1 = Candidate.builder()
                .id(candidate1Id)
                .tenantId(tenantId)
                .fullName("John Doe")
                .emailAddress("john.doe@example.com")
                .phoneNumber("+1-555-0123")
                .build();

        candidate2 = Candidate.builder()
                .id(candidate2Id)
                .tenantId(tenantId)
                .fullName("John Doe")
                .emailAddress("john.doe@example.com")
                .phoneNumber("+1-555-0123")
                .build();
    }

    @Test
    @DisplayName("Should detect duplicate candidates with same email")
    void testDetectDuplicates_SameEmail() {
        List<Candidate> existingCandidates = new ArrayList<>();
        existingCandidates.add(candidate2);

        ResumeMetadata resume1 = ResumeMetadata.builder()
                .candidateId(candidate1Id)
                .build();
        ResumeMetadata resume2 = ResumeMetadata.builder()
                .candidateId(candidate2Id)
                .build();

        when(candidateRepository.findById(candidate1Id)).thenReturn(Optional.of(candidate1));
        when(resumeMetadataRepository.findByCandidateId(candidate1Id))
                .thenReturn(List.of(resume1));
        when(candidateRepository.findByTenantId(tenantId)).thenReturn(existingCandidates);
        when(resumeMetadataRepository.findByCandidateId(candidate2Id))
                .thenReturn(List.of(resume2));

        List<DuplicateCandidate> duplicates = duplicateDetectionService.detectDuplicates(
                candidate1Id,
                tenantId
        );

        assertNotNull(duplicates);
        assertEquals(1, duplicates.size());
        assertEquals("Same email address", duplicates.get(0).getDetectedReason());
    }

    @Test
    @DisplayName("Should not flag different candidates as duplicates")
    void testDetectDuplicates_NoDuplicates() {
        Candidate differentCandidate = Candidate.builder()
                .id(candidate2Id)
                .tenantId(tenantId)
                .fullName("Jane Smith")
                .emailAddress("jane.smith@example.com")
                .phoneNumber("+1-555-9999")
                .build();

        List<Candidate> existingCandidates = new ArrayList<>();
        existingCandidates.add(differentCandidate);

        ResumeMetadata resume1 = ResumeMetadata.builder()
                .candidateId(candidate1Id)
                .build();

        when(candidateRepository.findById(candidate1Id)).thenReturn(Optional.of(candidate1));
        when(resumeMetadataRepository.findByCandidateId(candidate1Id))
                .thenReturn(List.of(resume1));
        when(candidateRepository.findByTenantId(tenantId)).thenReturn(existingCandidates);

        List<DuplicateCandidate> duplicates = duplicateDetectionService.detectDuplicates(
                candidate1Id,
                tenantId
        );

        assertNotNull(duplicates);
        assertEquals(0, duplicates.size());
    }

    @Test
    @DisplayName("Should merge duplicate candidates")
    void testMergeDuplicates_Success() {
        DuplicateCandidate duplicate = DuplicateCandidate.builder()
                .primaryCandidateId(candidate1Id)
                .duplicateCandidateId(candidate2Id)
                .build();

        List<DuplicateCandidate> duplicateList = new ArrayList<>();
        duplicateList.add(duplicate);

        when(duplicateCandidateRepository.findByPrimaryCandidateId(candidate1Id))
                .thenReturn(duplicateList);

        duplicateDetectionService.mergeDuplicates(candidate1Id, candidate2Id);

        verify(duplicateCandidateRepository, times(1)).save(any(DuplicateCandidate.class));
        verify(candidateRepository, times(1)).deleteById(candidate2Id);
    }
}
