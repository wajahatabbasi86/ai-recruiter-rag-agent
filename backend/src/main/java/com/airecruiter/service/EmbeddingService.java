package com.airecruiter.service;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points.PointStruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;

/**
 * Service for generating embeddings and storing them in Qdrant.
 * Uses Spring AI with Ollama embedding model.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final QdrantClient qdrantClient;

    @Qualifier("ollamaEmbeddingModel")
    private final EmbeddingModel embeddingModel;

    @Value("${qdrant.collections.resumes}")
    private String resumesCollection;

    @Value("${qdrant.collections.jobs}")
    private String jobsCollection;

    @Value("${spring.ai.ollama.embedding.model}")
    private String embeddingModelName;

    /**
     * Embeds a text string using the embedding model.
     *
     * @param textToEmbed the text to embed
     * @return embedding vector
     */
    public float[] embedText(String textToEmbed) {
        try {
            log.debug("Embedding text of length: {}", textToEmbed.length());
            
            float[] embedding = embeddingModel.embed(textToEmbed);
            
            log.debug("Text embedded successfully, vector dimension: {}", embedding.length);
            
            return embedding;
            
        } catch (Exception exception) {
            log.error("Error during text embedding: {}", exception.getMessage(), exception);
            throw new RuntimeException("Failed to embed text", exception);
        }
    }

    /**
     * Stores a resume embedding in Qdrant.
     *
     * @param candidateId the candidate ID
     * @param resumeText the resume text
     * @param section the resume section
     * @return the point ID
     */
    public String storeResumeEmbedding(UUID candidateId, String resumeText, String section) {
        try {
            log.debug("Storing resume embedding for candidate: {}, section: {}", candidateId, section);
            
            float[] embedding = embedText(resumeText);
            String pointId = UUID.randomUUID().toString();
            
            PointStruct point = PointStruct.newBuilder()
                    .setId(id(UUID.fromString(pointId)))
                    .setVectors(vectors(convertToFloatList(embedding)))
                    .putAllPayload(Map.of(
                            "candidateId", value(candidateId.toString()),
                            "section", value(section),
                            "text", value(resumeText),
                            "model", value(embeddingModelName)
                    ))
                    .build();
            
            qdrantClient.upsertAsync(resumesCollection, List.of(point)).get();
            
            log.debug("Resume embedding stored with point ID: {}", pointId);
            
            return pointId;
            
        } catch (Exception exception) {
            log.error("Error storing resume embedding: {}", exception.getMessage(), exception);
            throw new RuntimeException("Failed to store resume embedding", exception);
        }
    }

    /**
     * Stores a job embedding in Qdrant.
     *
     * @param jobId the job ID
     * @param jobText the job text
     * @param section the job section
     * @return the point ID
     */
    public String storeJobEmbedding(UUID jobId, String jobText, String section) {
        try {
            log.debug("Storing job embedding for job: {}, section: {}", jobId, section);
            
            float[] embedding = embedText(jobText);
            String pointId = UUID.randomUUID().toString();
            
            PointStruct point = PointStruct.newBuilder()
                    .setId(id(UUID.fromString(pointId)))
                    .setVectors(vectors(convertToFloatList(embedding)))
                    .putAllPayload(Map.of(
                            "jobId", value(jobId.toString()),
                            "section", value(section),
                            "text", value(jobText),
                            "model", value(embeddingModelName)
                    ))
                    .build();
            
            qdrantClient.upsertAsync(jobsCollection, List.of(point)).get();
            
            log.debug("Job embedding stored with point ID: {}", pointId);
            
            return pointId;
            
        } catch (Exception exception) {
            log.error("Error storing job embedding: {}", exception.getMessage(), exception);
            throw new RuntimeException("Failed to store job embedding", exception);
        }
    }

    /**
     * Converts float array to List<Float>.
     *
     * @param floatArray the float array
     * @return list of floats
     */
    private List<Float> convertToFloatList(float[] floatArray) {
        List<Float> floatList = new ArrayList<>(floatArray.length);
        for (float value : floatArray) {
            floatList.add(value);
        }
        return floatList;
    }
}
