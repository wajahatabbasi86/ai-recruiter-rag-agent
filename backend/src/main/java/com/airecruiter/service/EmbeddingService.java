package com.airecruiter.service;

import com.airecruiter.service.JobParserService.JobChunk;
import com.airecruiter.service.ResumeParserService.ResumeChunk;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points.PointStruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;

/**
 * Generates embeddings via Ollama and stores them in Qdrant.
 *
 * Spring AI injects OllamaEmbeddingModel automatically.
 * Both resume chunks and job chunks are embedded the same way —
 * only the target collection differs.
 *
 * Returns the list of Qdrant point IDs so callers can persist them
 * for future cleanup.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final QdrantClient qdrantClient;

    @Value("${qdrant.collections.resumes}")
    private String resumeCollection;

    @Value("${qdrant.collections.jobs}")
    private String jobCollection;

    @Value("${spring.ai.ollama.embedding.model}")
    private String embeddingModelName;

    /**
     * Embeds all chunks of a resume and stores them in Qdrant.
     * Returns list of point IDs (one per chunk).
     */
    @Async
    public CompletableFuture<List<String>> embedAndStoreResume(UUID candidateId, List<ResumeChunk> chunks) {
        List<PointStruct> points = new ArrayList<>();
        List<String> pointIds = new ArrayList<>();

        for (ResumeChunk chunk : chunks) {
            try {
                float[] vector = embeddingModel.embed(chunk.toEmbeddingText());
                String pointId = UUID.randomUUID().toString();

                points.add(PointStruct.newBuilder()
                    .setId(id(UUID.fromString(pointId)))
                    .setVectors(vectors(toFloatList(vector)))
                    .putAllPayload(Map.of(
                        "candidateId", value(candidateId.toString()),
                        "section",     value(chunk.section()),
                        "text",        value(chunk.text()),
                        "model",       value(embeddingModelName)
                    ))
                    .build());
                pointIds.add(pointId);
            } catch (Exception e) {
                log.error("Failed to embed resume chunk for candidate {}: {}", candidateId, e.getMessage());
            }
        }

        upsert(resumeCollection, points, candidateId.toString());
        return CompletableFuture.completedFuture(pointIds);
    }

    /**
     * Embeds all chunks of a job description and stores them in Qdrant.
     * Returns list of point IDs (one per chunk).
     */
    @Async
    public CompletableFuture<List<String>> embedAndStoreJob(UUID jobId, List<JobChunk> chunks) {
        List<PointStruct> points = new ArrayList<>();
        List<String> pointIds = new ArrayList<>();

        for (JobChunk chunk : chunks) {
            try {
                float[] vector = embeddingModel.embed(chunk.toEmbeddingText());
                String pointId = UUID.randomUUID().toString();

                points.add(PointStruct.newBuilder()
                    .setId(id(UUID.fromString(pointId)))
                    .setVectors(vectors(toFloatList(vector)))
                    .putAllPayload(Map.of(
                        "jobId",   value(jobId.toString()),
                        "section", value(chunk.section()),
                        "text",    value(chunk.text()),
                        "model",   value(embeddingModelName)
                    ))
                    .build());
                pointIds.add(pointId);
            } catch (Exception e) {
                log.error("Failed to embed job chunk for job {}: {}", jobId, e.getMessage());
            }
        }

        upsert(jobCollection, points, jobId.toString());
        return CompletableFuture.completedFuture(pointIds);
    }

    /** Embeds a query string for use in RAG search. */
    public float[] embedQuery(String query) {
        return embeddingModel.embed(query);
    }

    private void upsert(String collection, List<PointStruct> points, String entityId) {
        if (points.isEmpty()) return;
        try {
            qdrantClient.upsertAsync(collection, points).get();
            log.info("Stored {} vectors in collection '{}' for entity {}",
                points.size(), collection, entityId);
        } catch (Exception e) {
            log.error("Failed to upsert vectors to '{}' for entity {}: {}",
                collection, entityId, e.getMessage());
        }
    }

    private List<Float> toFloatList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float f : array) list.add(f);
        return list;
    }
}
