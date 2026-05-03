package com.llmwiki.adapter.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmwiki.adapter.dto.ExampleData;
import com.llmwiki.adapter.dto.ExtractionResult;
import com.llmwiki.adapter.dto.ExtractionResult.ConceptInfo;
import com.llmwiki.adapter.dto.ExtractionResult.EntityInfo;
import com.llmwiki.adapter.dto.ScoreResult;
import com.llmwiki.adapter.prompting.PromptTemplate;
import com.llmwiki.adapter.resolver.AlignmentResolver;
import com.llmwiki.common.enums.AlignmentStatus;
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

import com.llmwiki.adapter.chunking.SlidingWindowChunker;
import com.llmwiki.adapter.chunking.TextChunk;

@Component
public class OpenAiApiClient implements AiApiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiApiClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final String model;
    private final AlignmentResolver alignmentResolver;
    private final SlidingWindowChunker chunker;

    private static final String SCORE_SYSTEM_PROMPT_DEFAULT = """
            You are a document quality analyzer. Score the document on these dimensions (0-10 each):
            1. information_density - How much useful information does the document contain per unit of text?
            2. entity_richness - How many notable entities (people, organizations, technologies, concepts) are mentioned?
            3. knowledge_independence - Can the document be understood as a standalone piece without external context?
            4. structure_integrity - Is the document well-organized with clear sections and logical flow?
            5. timeliness - Is the information current and up-to-date?

            Respond in JSON format:
            {"scores":{"information_density":N,"entity_richness":N,"knowledge_independence":N,"structure_integrity":N,"timeliness":N},"overall_score":N,"reason":"explanation","key_entities":["entity1","entity2"],"suggested_tags":["tag1","tag2"]}
            """;

    private static final String ENTITY_SYSTEM_PROMPT_DEFAULT = """
            Extract named entities from the text. Identify people, organizations, technologies,
            tools, and other important named things. Do NOT include abstract concepts — those are handled separately.

            For each entity, provide the character position (start_offset, end_offset) where the entity
            appears in the source text. If the entity appears multiple times, use the first occurrence.

            Also identify relationships between entities found in the text.

            Respond in JSON format:
            {"entities":[{"name":"entity_name","type":"PERSON|ORG|TECH|TOOL|OTHER","description":"brief description","start_offset":0,"end_offset":10,"related_entities":["other_entity_name"]}]}
            """;

    private static final String CONCEPT_SYSTEM_PROMPT_DEFAULT = """
            Extract key concepts and themes from the text. A concept is an abstract idea or topic.

            For each concept, provide the character position (start_offset, end_offset) where the concept
            appears in the source text. If the concept appears multiple times, use the first occurrence.

            Respond in JSON format:
            {"concepts":[{"name":"concept_name","description":"brief description","start_offset":0,"end_offset":10,"related_entities":["entity1","entity2"]}]}
            """;

    private final String scoreSystemPrompt;
    private final String entitySystemPrompt;
    private final String conceptSystemPrompt;

    public OpenAiApiClient(
            @Value("${ai.api.base-url:http://localhost:8000/v1}") String baseUrl,
            @Value("${ai.api.key:sk-local}") String apiKey,
            @Value("${ai.model:gpt-4o-mini}") String model,
            @Value("${ai.prompt.score:}") String scorePrompt,
            @Value("${ai.prompt.entity:}") String entityPrompt,
            @Value("${ai.prompt.concept:}") String conceptPrompt,
            AlignmentResolver alignmentResolver) {
        this.model = model;
        this.alignmentResolver = alignmentResolver;
        this.chunker = new SlidingWindowChunker(8000, 200);
        this.scoreSystemPrompt = scorePrompt.isEmpty() ? SCORE_SYSTEM_PROMPT_DEFAULT : scorePrompt;
        this.entitySystemPrompt = entityPrompt.isEmpty() ? ENTITY_SYSTEM_PROMPT_DEFAULT : entityPrompt;
        this.conceptSystemPrompt = conceptPrompt.isEmpty() ? CONCEPT_SYSTEM_PROMPT_DEFAULT : conceptPrompt;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public ScoreResult score(String content) {
        try {
            String response = callApi(scoreSystemPrompt, content);
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
        return extractEntities(content, null);
    }

    @Override
    public ExtractionResult extractEntities(String content, List<ExampleData> examples) {
        try {
            String systemPrompt = buildFewShotPrompt(entitySystemPrompt, examples);
            // Use sliding window chunking with sentence boundary awareness
            List<TextChunk> textChunks = chunker.chunk(content);
            List<String> chunks = new ArrayList<>();
            for (TextChunk tc : textChunks) {
                chunks.add(tc.getText());
            }
            ExtractionResult merged = new ExtractionResult();
            List<EntityInfo> allEntities = new ArrayList<>();
            int extractionIdx = 0;

            for (String chunk : chunks) {
                String response = callApi(systemPrompt, chunk);
                JsonNode root = MAPPER.readTree(response);
                JsonNode arr = root.get("entities");
                if (arr != null && arr.isArray()) {
                    for (JsonNode node : arr) {
                        List<String> related = new ArrayList<>();
                        JsonNode rel = node.get("related_entities");
                        if (rel != null && rel.isArray()) {
                            for (JsonNode r : rel) related.add(r.asText());
                        }
                        String name = node.get("name").asText();
                        EntityInfo entity = new EntityInfo(
                                name,
                                node.get("type").asText(),
                                node.has("description") ? node.get("description").asText() : "",
                                related);

                        // Parse character positions from LLM response
                        if (node.has("start_offset") && node.has("end_offset")) {
                            entity.setStartOffset(node.get("start_offset").asInt());
                            entity.setEndOffset(node.get("end_offset").asInt());
                            entity.setAlignmentStatus(AlignmentStatus.EXACT);
                        }

                        entity.setExtractionIndex(extractionIdx++);
                        allEntities.add(entity);
                    }
                }
            }

            // Deduplicate by name (keep first occurrence)
            Map<String, EntityInfo> deduped = new LinkedHashMap<>();
            for (EntityInfo e : allEntities) {
                String key = e.getName().toLowerCase();
                if (!deduped.containsKey(key)) {
                    deduped.put(key, e);
                }
            }

            // Apply alignment resolver for entities without positions
            List<EntityInfo> finalEntities = new ArrayList<>();
            for (EntityInfo e : deduped.values()) {
                if (e.getStartOffset() == null) {
                    AlignmentResolver.AlignmentResult aligned =
                            alignmentResolver.alignEntity(e.getName(), content);
                    if (aligned != null) {
                        e.setStartOffset(aligned.getStartOffset());
                        e.setEndOffset(aligned.getEndOffset());
                        e.setAlignmentStatus(aligned.getStatus());
                    }
                }
                finalEntities.add(e);
            }

            merged.setEntities(finalEntities);
            merged.setConcepts(Collections.emptyList());
            return merged;
        } catch (Exception e) {
            log.error("Failed to extract entities", e);
            return new ExtractionResult();
        }
    }

    @Override
    public ExtractionResult extractConcepts(String content) {
        return extractConcepts(content, null);
    }

    @Override
    public ExtractionResult extractConcepts(String content, List<ExampleData> examples) {
        try {
            String systemPrompt = buildFewShotPrompt(conceptSystemPrompt, examples);
            List<TextChunk> textChunks = chunker.chunk(content);
            List<String> chunks = new ArrayList<>();
            for (TextChunk tc : textChunks) {
                chunks.add(tc.getText());
            }
            ExtractionResult merged = new ExtractionResult();
            List<ConceptInfo> allConcepts = new ArrayList<>();
            int extractionIdx = 0;

            for (String chunk : chunks) {
                String response = callApi(systemPrompt, chunk);
                JsonNode root = MAPPER.readTree(response);
                JsonNode arr = root.get("concepts");
                if (arr != null && arr.isArray()) {
                    for (JsonNode node : arr) {
                        List<String> related = new ArrayList<>();
                        JsonNode rel = node.get("related_entities");
                        if (rel != null && rel.isArray()) {
                            for (JsonNode r : rel) related.add(r.asText());
                        }
                        String name = node.get("name").asText();
                        ConceptInfo concept = new ConceptInfo(
                                name,
                                node.has("description") ? node.get("description").asText() : "",
                                related);

                        // Parse character positions from LLM response
                        if (node.has("start_offset") && node.has("end_offset")) {
                            concept.setStartOffset(node.get("start_offset").asInt());
                            concept.setEndOffset(node.get("end_offset").asInt());
                            concept.setAlignmentStatus(AlignmentStatus.EXACT);
                        }

                        concept.setExtractionIndex(extractionIdx++);
                        allConcepts.add(concept);
                    }
                }
            }

            // Deduplicate by name
            Map<String, ConceptInfo> deduped = new LinkedHashMap<>();
            for (ConceptInfo c : allConcepts) {
                String key = c.getName().toLowerCase();
                if (!deduped.containsKey(key)) {
                    deduped.put(key, c);
                }
            }

            // Apply alignment resolver for concepts without positions
            List<ConceptInfo> finalConcepts = new ArrayList<>();
            for (ConceptInfo c : deduped.values()) {
                if (c.getStartOffset() == null) {
                    AlignmentResolver.AlignmentResult aligned =
                            alignmentResolver.alignEntity(c.getName(), content);
                    if (aligned != null) {
                        c.setStartOffset(aligned.getStartOffset());
                        c.setEndOffset(aligned.getEndOffset());
                        c.setAlignmentStatus(aligned.getStatus());
                    }
                }
                finalConcepts.add(c);
            }

            merged.setEntities(Collections.emptyList());
            merged.setConcepts(finalConcepts);
            return merged;
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

    /**
     * Build a few-shot prompt from a base system prompt and examples.
     * Falls back to the base prompt when no examples are provided.
     */
    private String buildFewShotPrompt(String basePrompt, List<ExampleData> examples) {
        if (examples == null || examples.isEmpty()) {
            return basePrompt;
        }
        PromptTemplate template = new PromptTemplate(basePrompt, examples);
        String rendered = template.render("{{INPUT}}");
        // Remove the trailing placeholder since the actual input is sent as the user message
        return rendered.substring(0, rendered.lastIndexOf("Text: {{INPUT}}"));
    }

    private String callApi(String systemPrompt, String userMessage) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)),
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
    }
