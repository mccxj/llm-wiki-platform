package com.llmwiki.adapter.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmwiki.adapter.dto.ExtractionResult;
import com.llmwiki.adapter.dto.ExtractionResult.ConceptInfo;
import com.llmwiki.adapter.dto.ExtractionResult.EntityInfo;
import com.llmwiki.adapter.dto.ScoreResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.*;

@Component
public class OpenAiApiClient implements AiApiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiApiClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final String model;

    private static final String SCORE_SYSTEM_PROMPT = """
            You are a document quality analyzer. Score the document on these dimensions (0-10 each):
            1. Relevance - How relevant is this to a knowledge base?
            2. Completeness - Does it cover the topic adequately?
            3. Accuracy - Is the information factually sound?
            4. Clarity - Is it well-written and understandable?
            5. Structure - Is it well-organized?
            
            Respond in JSON format:
            {"scores":{"relevance":N,"completeness":N,"accuracy":N,"clarity":N,"structure":N},"overall_score":N,"reason":"explanation","key_entities":["entity1","entity2"],"suggested_tags":["tag1","tag2"]}
            """;

    private static final String ENTITY_SYSTEM_PROMPT = """
            Extract named entities from the text. Identify people, organizations, technologies, 
            concepts, tools, and other important named things.
            
            Respond in JSON format:
            {"entities":[{"name":"entity_name","type":"PERSON|ORG|TECH|CONCEPT|TOOL|OTHER","description":"brief description"}]}
            """;

    private static final String CONCEPT_SYSTEM_PROMPT = """
            Extract key concepts and themes from the text. A concept is an abstract idea or topic.
            
            Respond in JSON format:
            {"concepts":[{"name":"concept_name","description":"brief description","related_entities":["entity1","entity2"]}]}
            """;

    public OpenAiApiClient(
            @Value("${ai.api.base-url:http://localhost:8000/v1}") String baseUrl,
            @Value("${ai.api.key:sk-local}") String apiKey,
            @Value("${ai.model:gpt-4o-mini}") String model) {
        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public ScoreResult score(String content) {
        try {
            String response = callApi(SCORE_SYSTEM_PROMPT, content);
            JsonNode root = MAPPER.readTree(response);

            ScoreResult result = new ScoreResult();
            Map<String, Integer> scores = new LinkedHashMap<>();
            JsonNode scoresNode = root.get("scores");
            if (scoresNode != null) {
                scoresNode.fields().forEachRemaining(e -> scores.put(e.getKey(), e.getValue().asInt()));
            }
            result.setScores(scores);

            JsonNode overall = root.get("overall_score");
            result.setOverallScore(overall != null ? BigDecimal.valueOf(overall.asDouble()) : BigDecimal.ZERO);
            result.setReason(root.has("reason") ? root.get("reason").asText() : "");

            result.setKeyEntities(parseStringList(root, "key_entities"));
            result.setSuggestedTags(parseStringList(root, "suggested_tags"));

            log.debug("Scored document: overall={}, scores={}", result.getOverallScore(), scores);
            return result;
        } catch (Exception e) {
            log.error("Failed to score document", e);
            ScoreResult fallback = new ScoreResult();
            fallback.setOverallScore(BigDecimal.ZERO);
            fallback.setReason("Scoring failed: " + e.getMessage());
            fallback.setScores(Collections.emptyMap());
            fallback.setKeyEntities(Collections.emptyList());
            fallback.setSuggestedTags(Collections.emptyList());
            return fallback;
        }
    }

    @Override
    public ExtractionResult extractEntities(String content) {
        try {
            String response = callApi(ENTITY_SYSTEM_PROMPT, content);
            JsonNode root = MAPPER.readTree(response);

            ExtractionResult result = new ExtractionResult();
            List<EntityInfo> entities = new ArrayList<>();
            JsonNode arr = root.get("entities");
            if (arr != null && arr.isArray()) {
                for (JsonNode node : arr) {
                    entities.add(new EntityInfo(
                            node.get("name").asText(),
                            node.get("type").asText(),
                            node.has("description") ? node.get("description").asText() : ""));
                }
            }
            result.setEntities(entities);
            result.setConcepts(Collections.emptyList());
            return result;
        } catch (Exception e) {
            log.error("Failed to extract entities", e);
            return new ExtractionResult();
        }
    }

    @Override
    public ExtractionResult extractConcepts(String content) {
        try {
            String response = callApi(CONCEPT_SYSTEM_PROMPT, content);
            JsonNode root = MAPPER.readTree(response);

            ExtractionResult result = new ExtractionResult();
            result.setEntities(Collections.emptyList());
            List<ConceptInfo> concepts = new ArrayList<>();
            JsonNode arr = root.get("concepts");
            if (arr != null && arr.isArray()) {
                for (JsonNode node : arr) {
                    List<String> related = new ArrayList<>();
                    JsonNode rel = node.get("related_entities");
                    if (rel != null && rel.isArray()) {
                        for (JsonNode r : rel) related.add(r.asText());
                    }
                    concepts.add(new ConceptInfo(
                            node.get("name").asText(),
                            node.has("description") ? node.get("description").asText() : "",
                            related));
                }
            }
            result.setConcepts(concepts);
            return result;
        } catch (Exception e) {
            log.error("Failed to extract concepts", e);
            return new ExtractionResult();
        }
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        return callApi(systemPrompt, userMessage);
    }

    @Override
    public boolean isAvailable() {
        try {
            webClient.get().uri("/models")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(5))
                    .onErrorReturn("")
                    .block();
            return true;
        } catch (Exception e) {
            log.warn("AI API not available at configured endpoint");
            return false;
        }
    }

    private String callApi(String systemPrompt, String userMessage) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", truncate(userMessage, 8000))),
                "temperature", 0.1,
                "max_tokens", 2000);

        Map<String, Object> response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(java.time.Duration.ofSeconds(60))
                .block();

        if (response == null) throw new RuntimeException("Empty response from AI API");

        List<?> choices = (List<?>) response.get("choices");
        if (choices == null || choices.isEmpty()) throw new RuntimeException("No choices in AI response");

        Map<?, ?> choice = (Map<?, ?>) choices.get(0);
        Map<?, ?> message = (Map<?, ?>) choice.get("message");
        return (String) message.get("content");
    }

    private List<String> parseStringList(JsonNode root, String field) {
        List<String> list = new ArrayList<>();
        JsonNode arr = root.get(field);
        if (arr != null && arr.isArray()) {
            for (JsonNode n : arr) list.add(n.asText());
        }
        return list;
    }

    private String truncate(String text, int maxLen) {
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }
}
