package com.catify.catify_api.config;

import io.qdrant.client.QdrantClient;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class QdrantHealthIndicator implements HealthIndicator {

    private final QdrantClient qdrantClient;          // bean from spring-ai-starter-vector-store-qdrant
    private final String collectionName;

    public QdrantHealthIndicator(QdrantClient qdrantClient,
                                 @org.springframework.beans.factory.annotation.Value(
                                         "${spring.ai.vectorstore.qdrant.collection-name}") String collectionName) {
        this.qdrantClient = qdrantClient;
        this.collectionName = collectionName;
    }

    @Override
    public Health health() {
        try {
            var info = qdrantClient.getCollectionInfoAsync(collectionName)
                    .get(2, TimeUnit.SECONDS);
            return Health.up()
                    .withDetail("collection", collectionName)
                    .withDetail("vectors", info.getIndexedVectorsCount())
                    .withDetail("status", info.getStatus().name())
                    .build();
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("collection", collectionName)
                    .build();
        }
    }
}
