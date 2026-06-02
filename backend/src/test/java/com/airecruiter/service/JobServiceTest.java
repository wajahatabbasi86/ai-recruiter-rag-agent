package com.airecruiter.service;

import com.airecruiter.dto.JobRequestDto;
import com.airecruiter.dto.JobResponseDto;
import com.airecruiter.model.Job;
import com.airecruiter.repository.JobRepository;
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
 * Unit tests for JobService.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JobService Unit Tests")
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ResumeExtractionService resumeExtractionService;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private QdrantClient qdrantClient;

    @InjectMocks
    private JobService jobService;

    private UUID defaultTenantId;
    private UUID jobId;
    private Job testJob;
    private JobRequestDto jobRequestDto;
    private MultipartFile mockFile;

    @BeforeEach
    void setUp() {
        defaultTenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        jobId = UUID.randomUUID();
        ReflectionTestUtils.setField(jobService, "defaultTenantId", defaultTenantId);
        ReflectionTestUtils.setField(jobService, "jobsCollection", "job-descriptions");

        testJob = Job.builder()
                .id(jobId)
                .tenantId(defaultTenantId)
                .jobTitle("Senior Java Developer")
                .jobDescription("Looking for experienced Java developer")
                .requiredSkills(new HashMap<String, String>() {{
                    put("java", "5+");
                    put("spring", "3+");
                }})
                .minimumExperienceYears(5)
                .requiredEducationLevel("Bachelor's")
                .requiredLocation("San Francisco, USA")
                .jobStatus("active")
                .build();

        jobRequestDto = JobRequestDto.builder()
                .jobTitle("Senior Java Developer")
                .jobDescription("Looking for experienced Java developer")
                .minimumExperienceYears(5)
                .requiredEducationLevel("Bachelor's")
                .requiredLocation("San Francisco, USA")
                .requiredSkills(testJob.getRequiredSkills())
                .build();
    }

    @Test
    @DisplayName("Should create job description successfully")
    void testCreateJobDescription_Success() {
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);

        JobResponseDto response = jobService.createJobDescription(jobRequestDto);

        assertNotNull(response);
        assertEquals(testJob.getId(), response.getJobId());
        assertEquals(testJob.getJobTitle(), response.getJobTitle());
        assertEquals("Job description created successfully", response.getMessage());
        verify(jobRepository, times(1)).save(any(Job.class));
    }

    @Test
    @DisplayName("Should retrieve all active jobs")
    void testGetAllActiveJobs_Success() {
        List<Job> activeJobs = new ArrayList<>();
        activeJobs.add(testJob);

        when(jobRepository.findByTenantIdAndJobStatus(defaultTenantId, "active"))
                .thenReturn(activeJobs);

        List<JobResponseDto> response = jobService.getAllActiveJobs();

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(testJob.getJobTitle(), response.get(0).getJobTitle());
        verify(jobRepository, times(1)).findByTenantIdAndJobStatus(defaultTenantId, "active");
    }

    @Test
    @DisplayName("Should retrieve job by ID")
    void testGetJobById_Success() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));

        JobResponseDto response = jobService.getJobById(jobId);

        assertNotNull(response);
        assertEquals(testJob.getId(), response.getJobId());
        assertEquals(testJob.getJobTitle(), response.getJobTitle());
        verify(jobRepository, times(1)).findById(jobId);
    }

    @Test
    @DisplayName("Should throw exception when job not found")
    void testGetJobById_NotFound() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> jobService.getJobById(jobId));
        verify(jobRepository, times(1)).findById(jobId);
    }

    @Test
    @DisplayName("Should update job description")
    void testUpdateJobDescription_Success() {
        JobRequestDto updateDto = JobRequestDto.builder()
                .jobTitle("Senior Java Developer - Updated")
                .minimumExperienceYears(6)
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);

        JobResponseDto response = jobService.updateJobDescription(jobId, updateDto);

        assertNotNull(response);
        verify(jobRepository, times(1)).findById(jobId);
        verify(jobRepository, times(1)).save(any(Job.class));
    }

    @Test
    @DisplayName("Should delete job successfully")
    void testDeleteJob_Success() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        doReturn(null).when(qdrantClient).deleteAsync(anyString(), any());

        jobService.deleteJob(jobId);

        verify(jobRepository, times(1)).findById(jobId);
        verify(jobRepository, times(1)).deleteById(jobId);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent job")
    void testDeleteJob_NotFound() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> jobService.deleteJob(jobId));
        verify(jobRepository, times(1)).findById(jobId);
    }
}
