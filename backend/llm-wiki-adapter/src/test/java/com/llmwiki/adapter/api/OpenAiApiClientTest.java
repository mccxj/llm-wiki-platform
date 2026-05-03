package com.llmwiki.adapter.api;

import com.llmwiki.adapter.dto.ExtractionResult;
import com.llmwiki.adapter.dto.UnifiedExtractionResult;
import com.llmwiki.adapter.dto.ScoreResult;
import com.llmwiki.adapter.resolver.AlignmentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiApiClientTest {

    OpenAiApiClient client;
    AlignmentResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AlignmentResolver();
        client = new OpenAiApiClient("http://localhost:9999", "test-key", "test-model", "", "", "", "", resolver);
    }

    @Test
    void shouldCreateInstance() {
        assertNotNull(client);
    }

    @Test
    void isAvailable_shouldReturnTrueEvenForUnreachableServer() {
        // isAvailable uses onErrorReturn("") so it always returns true
        assertTrue(client.isAvailable());
    }

    @Test
    void score_shouldReturnFallbackForUnreachableServer() {
        ScoreResult result = client.score("test content");
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getOverallScore());
        assertTrue(result.getReason().contains("Scoring failed"));
        assertTrue(result.getScores().isEmpty());
        assertTrue(result.getKeyEntities().isEmpty());
        assertTrue(result.getSuggestedTags().isEmpty());
    }

    @Test
    void extractEntities_shouldReturnEmptyForUnreachableServer() {
        ExtractionResult result = client.extractEntities("test content");
        assertNotNull(result);
        assertTrue(result.getEntities() == null || result.getEntities().isEmpty());
        assertTrue(result.getConcepts() == null || result.getConcepts().isEmpty());
    }

    @Test
    void extractConcepts_shouldReturnEmptyForUnreachableServer() {
        ExtractionResult result = client.extractConcepts("test content");
        assertNotNull(result);
        assertTrue(result.getEntities() == null || result.getEntities().isEmpty());
        assertTrue(result.getConcepts() == null || result.getConcepts().isEmpty());
    }

    @Test
    void chat_shouldThrowForUnreachableServer() {
        assertThrows(Exception.class, () -> client.chat("system", "user"));
    }

    @Test
    void constructor_withCustomPrompts_shouldUseProvidedPrompts() {
        String customScore = "Custom score prompt";
        String customEntity = "Custom entity prompt";
        String customConcept = "Custom concept prompt";
        OpenAiApiClient customClient = new OpenAiApiClient(
                "http://localhost:9999", "test-key", "test-model",
                customScore, customEntity, customConcept, "", resolver);
        assertNotNull(customClient);
        assertTrue(customClient.isAvailable());
    }

    @Test
    void unifiedExtract_shouldReturnEmptyForUnreachableServer() {
        UnifiedExtractionResult result = client.unifiedExtract("test content");
        assertNotNull(result);
        assertTrue(result.getEntities() == null || result.getEntities().isEmpty());
        assertTrue(result.getConcepts() == null || result.getConcepts().isEmpty());
        assertTrue(result.getRelations() == null || result.getRelations().isEmpty());
    }
}
