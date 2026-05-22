package com.airecruiter.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class QdrantConfig {

    @Value("${qdrant.host}")
    private String host;

    @Value("${qdrant.port}")
    private int port;

    @Value("${qdrant.collections.resumes}")
    private String resumeCollection;

    @Value("${qdrant.collections.jobs}")
    private String jobCollection;

    @Value("${qdrant.vector-size}")
    private int vectorSize;

    @Bean
    public QdrantClient qdrantClient() {
        QdrantClient client = new QdrantClient(
                QdrantGrpcClient.newBuilder(host, port, false).build()
        );
        // Initialize collections here directly — no @PostConstruct needed
        createCollectionIfAbsent(client, resumeCollection);
        createCollectionIfAbsent(client, jobCollection);
        return client;
    }

    private void createCollectionIfAbsent(QdrantClient client, String collectionName) {
        try {
            var collections = client.listCollectionsAsync().get();
            boolean exists = collections.stream()
                    .anyMatch(c -> c.equals(collectionName));

            if (!exists) {
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
        } catch (Exception e) {
            log.error("Failed to initialize Qdrant collection '{}': {}", collectionName, e.getMessage());
        }
    }
}