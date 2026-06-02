package com.airecruiter.service;

import io.qdrant.client.QdrantClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EmbeddingService.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmbeddingService Unit Tests")
class EmbeddingServiceTest {

    @Mock
    private QdrantClient qdrantClient;

    @Mock
    private EmbeddingModel embeddingModel;

    @InjectMocks
    private EmbeddingService embeddingService;

    private String textToEmbed;
    private float[] mockEmbedding;

    @BeforeEach
    void setUp() {
        textToEmbed = "This is a sample text to embed for testing";
        mockEmbedding = new float[768]; // nomic-embed-text dimension
        for (int i = 0; i < mockEmbedding.length; i++) {
            mockEmbedding[i] = (float) Math.random();
        }

        ReflectionTestUtils.setField(embeddingService, "resumesCollection", "resume-chunks");
        ReflectionTestUtils.setField(embeddingService, "jobsCollection", "job-descriptions");
        ReflectionTestUtils.setField(embeddingService, "embeddingModelName", "nomic-embed-text");
    }

    @Test
    @DisplayName("Should embed text successfully")
    void testEmbedText_Success() {
        when(embeddingModel.embed(textToEmbed)).thenReturn(mockEmbedding);

        float[] result = embeddingService.embedText(textToEmbed);

        assertNotNull(result);
        assertTrue(result.length > 0);
        verify(embeddingModel, times(1)).embed(textToEmbed);
    }

    @Test
    @DisplayName("Should store resume embedding in Qdrant")
    void testStoreResumeEmbedding_Success() {
        String resumeText = "Senior Java Developer with 5 years experience";
        var candidateId = java.util.UUID.randomUUID();

        when(embeddingModel.embed(any())).thenReturn(mockEmbedding);
        when(qdrantClient.upsertAsync(any(), any())).thenReturn(new java.util.concurrent.CompletableFuture<>() {{
            complete(null);
        }});

        String pointId = embeddingService.storeResumeEmbedding(candidateId, resumeText, "skills");

        assertNotNull(pointId);
        verify(embeddingModel, times(1)).embed(any());
        verify(qdrantClient, times(1)).upsertAsync(any(), any());
    }

    @Test
    @DisplayName("Should store job embedding in Qdrant")
    void testStoreJobEmbedding_Success() {
        String jobText = "Looking for a senior Java developer with Spring Boot experience";
        var jobId = java.util.UUID.randomUUID();

        when(embeddingModel.embed(any())).thenReturn(mockEmbedding);
        when(qdrantClient.upsertAsync(any(), any())).thenReturn(new java.util.concurrent.CompletableFuture<>() {{
            complete(null);
        }});

        String pointId = embeddingService.storeJobEmbedding(jobId, jobText, "requirements");

        assertNotNull(pointId);
        verify(embeddingModel, times(1)).embed(any());
        verify(qdrantClient, times(1)).upsertAsync(any(), any());
    }
}
