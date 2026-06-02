package com.airecruiter.service;

import com.airecruiter.dto.ResumeMetadataDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tika.Tika;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ResumeExtractionService.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeExtractionService Unit Tests")
class ResumeExtractionServiceTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Tika tikaParser;

    @InjectMocks
    private ResumeExtractionService resumeExtractionService;

    private MultipartFile mockFile;
    private String resumeText;

    @BeforeEach
    void setUp() {
        mockFile = mock(MultipartFile.class);
        resumeText = """John Doe
                Senior Software Engineer
                Experience: 5 years in Java
                Skills: Java, Spring Boot, AWS
                Education: Bachelor's in Computer Science""";
    }

    @Test
    @DisplayName("Should extract text from PDF file")
    void testExtractTextFromFile_Success() throws IOException {
        when(mockFile.getOriginalFilename()).thenReturn("resume.pdf");
        when(tikaParser.parseToString(any())).thenReturn(resumeText);

        String extractedText = resumeExtractionService.extractTextFromFile(mockFile);

        assertNotNull(extractedText);
        assertEquals(resumeText, extractedText);
        verify(tikaParser, times(1)).parseToString(any());
    }

    @Test
    @DisplayName("Should extract structured metadata from resume text")
    void testExtractStructuredMetadata_Success() throws Exception {
        String jsonResponse = """{
            "technical_skills": {"java": 5, "spring": 3},
            "total_experience_years": 5,
            "education_level": "Bachelor's",
            "university": "MIT",
            "current_location": "San Francisco, USA",
            "willing_to_relocate": true,
            "tools": {"docker": true, "kubernetes": false},
            "frameworks": ["Spring Boot", "React"],
            "databases": ["PostgreSQL", "MongoDB"],
            "latest_company": "Google",
            "latest_job_title": "Senior Engineer",
            "employment_status": "Employed",
            "email": "john.doe@example.com",
            "phone": "+1-555-0123"
        }""";

        var mockGeneration = mock(Generation.class);
        var mockResult = mock(org.springframework.ai.chat.ChatResult.class);

        when(chatModel.call(any(Prompt.class))).thenReturn(mockResult);
        when(mockResult.getResult()).thenReturn(mockGeneration);
        when(mockGeneration.getOutput()).thenReturn(new org.springframework.ai.chat.model.AssistantMessage(jsonResponse));

        ResumeMetadataDto metadata = resumeExtractionService.extractStructuredMetadata(resumeText);

        assertNotNull(metadata);
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    @DisplayName("Should handle extraction gracefully when LLM fails")
    void testExtractStructuredMetadata_LLMFailure() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("LLM error"));

        ResumeMetadataDto metadata = resumeExtractionService.extractStructuredMetadata(resumeText);

        assertNotNull(metadata);
        assertEquals(0, metadata.getTotalExperienceYears());
        verify(chatModel, times(1)).call(any(Prompt.class));
    }
}
