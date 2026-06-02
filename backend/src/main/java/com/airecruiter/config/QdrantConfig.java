package com.airecruiter.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Qdrant vector database.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@Slf4j
@Configuration
public class QdrantConfig {

    @Value("${qdrant.host}")
    private String qdrantHost;

    @Value("${qdrant.port}")
    private Integer qdrantPort;

    @Value("${qdrant.collections.resumes}")
    private String resumesCollection;

    @Value("${qdrant.collections.jobs}")
    private String jobsCollection;

    @Value("${qdrant.vector-size}")
    private Integer vectorSize;

    /**
     * Creates Qdrant client bean.
     *
     * @return Qdrant client
     */
    @Bean
    public QdrantClient qdrantClient() {
        log.info("Initializing Qdrant client: {}:{}", qdrantHost, qdrantPort);
        
        QdrantClient client = new QdrantClient(
                QdrantGrpcClient.newBuilder(qdrantHost, qdrantPort, false).build()
        );
        
        initializeCollections(client);
        
        return client;
    }

    /**
     * Initializes required Qdrant collections.
     *
     * @param client the Qdrant client
     */
    private void initializeCollections(QdrantClient client) {
        try {
            createCollectionIfNotExists(client, resumesCollection);
            createCollectionIfNotExists(client, jobsCollection);
        } catch (Exception exception) {
            log.error("Error initializing Qdrant collections: {}", exception.getMessage(), exception);
        }
    }

    /**
     * Creates a collection if it does not already exist.
     *
     * @param client the Qdrant client
     * @param collectionName the collection name
     */
    private void createCollectionIfNotExists(QdrantClient client, String collectionName) {
        try {
            var collections = client.listCollectionsAsync().get();
            boolean collectionExists = collections.stream()
                    .anyMatch(c -> c.equals(collectionName));

            if (!collectionExists) {
                client.createCollectionAsync(
                        collectionName,
                        VectorParams.newBuilder()
                                .setSize(vectorSize)
                                .setDistance(Distance.Cosine)
                                .build()
                ).get();
                log.info("Created Qdrant collection: {}", collectionName);
            } else {
                log.info("Qdrant collection already exists: {}", collectionName);
            }
        } catch (Exception exception) {
            log.error("Error creating Qdrant collection '{}': {}",
                    collectionName, exception.getMessage(), exception);
        }
    }
}
