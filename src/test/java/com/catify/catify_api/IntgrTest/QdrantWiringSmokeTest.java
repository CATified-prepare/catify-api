package com.catify.catify_api.IntgrTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("integration-test")
public class QdrantWiringSmokeTest {

    @Autowired
    VectorStore vectorStore;

    private final List<String> insertedIds = new java.util.ArrayList<>();

    @Test
    void shouldEmbedCatQuestionsAndRetrieveBySemanticMeaning() {
        Document quantDoc = new Document(
                "Quantitative Aptitude: A train travels 60 km/h for 2 hours then 80 km/h for 3 hours. " +
                        "What is the average speed for the entire journey? Answer: 72 km/h.",
                Map.of("year", 2022, "topic", "quant", "difficulty", "easy", "source", "smoke-test")
        );
        Document varcDoc = new Document(
                "VARC Reading Comprehension: The passage argues that cognitive biases " +
                        "affect economic decisions and humans are not rational actors. " +
                        "Question: What is the central argument? Answer: Humans use biases, not logic.",
                Map.of("year", 2023, "topic", "varc", "difficulty", "medium", "source", "smoke-test")
        );
        Document dilrDoc = new Document(
                "DILR: A bar chart shows sales of 4 companies over 5 years. " +
                        "Company A grew 20% YoY. Which company had highest absolute growth? Answer: Company A.",
                Map.of("year", 2023, "topic", "dilr", "difficulty", "hard", "source", "smoke-test")
        );

        vectorStore.add(List.of(quantDoc, varcDoc, dilrDoc));
        insertedIds.addAll(List.of(quantDoc.getId(), varcDoc.getId(), dilrDoc.getId()));

        List<Document> quantResults = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("speed distance time problem in CAT quant")
                        .topK(1)
                        .similarityThreshold(0.5)
                        .build()
        );
        assertThat(quantResults).isNotEmpty();
        assertThat(quantResults.getFirst().getMetadata()).containsEntry("topic", "quant");

        List<Document> varcResults = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("reading comprehension passage about human behaviour")
                        .topK(1)
                        .similarityThreshold(0.5)
                        .build()
        );
        assertThat(varcResults).isNotEmpty();
        assertThat(varcResults.getFirst().getMetadata()).containsEntry("topic", "varc");
    }

    @Test
    void shouldReturnNoResultsForCompletelyUnrelatedQuery() {
        // Insert one CAT quant question about profit and loss
        Document quantDoc = new Document(
                "Quantitative Aptitude: A shopkeeper bought an item for Rs 800 and sold it for Rs 1000. " +
                        "What is the profit percentage? Answer: Profit = 200, Profit% = 25%.",
                Map.of("year", 2021, "topic", "quant", "difficulty", "easy", "source", "smoke-test")
        );
        vectorStore.add(List.of(quantDoc));
        insertedIds.add(quantDoc.getId());

        // Search with a very high threshold + completely unrelated query (recipe, not CAT)
        // Similarity score will be far below 0.99 → no results returned
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("how to bake chocolate cake with butter and eggs")
                        .topK(5)
                        .similarityThreshold(0.99)   // extremely strict — nothing will match
                        .build()
        );

        // ASSERT: unrelated query returns empty — proves threshold filtering works
        assertThat(results).isEmpty();
    }

    @AfterEach
    void cleanUp() {
        if (!insertedIds.isEmpty()) {
            vectorStore.delete(insertedIds);
            insertedIds.clear();
        }
    }
}
