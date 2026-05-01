package com.llmwiki.adapter.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiEmbeddingClient implements EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiEmbeddingClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final String model;
    private final int dimension;

    public OpenAiEmbeddingClient(
            @Value("${ai.api.base-url:http://localhost:8000/v1}") String baseUrl,
            @Value("${ai.api.key:sk-local}") String apiKey,
            @Value("${embedding.model:text-embedding-ada-002}") String model,
            @Value("${embedding.dimension:1536}") int dimension) {
        this.model = model;
        this.dimension = dimension;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public float[] embed(String text) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "input", text);

            Map<?, ?> response = webClient.post()
                    .uri("/embeddings")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(java.time.Duration.ofSeconds(30))
                    .block();

            if (response == null) throw new RuntimeException("Empty response from embedding API");

            List<?> data = (List<?>) response.get("data");
            if (data == null || data.isEmpty()) throw new RuntimeException("No embedding data in response");

            Map<?, ?> item = (Map<?, ?>) data.get(0);
            List<?> embedding = (List<?>) item.get("embedding");

            float[] result = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                result[i] = ((Number) embedding.get(i)).floatValue();
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to generate embedding", e);
            return new float[dimension];
        }
    }

    @Override
    public int getDimension() {
        return dimension;
    }
}
