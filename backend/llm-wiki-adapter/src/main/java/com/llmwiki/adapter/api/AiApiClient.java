package com.llmwiki.adapter.api;

import com.llmwiki.adapter.dto.ExampleData;
import com.llmwiki.adapter.dto.ExtractionResult;
import com.llmwiki.adapter.dto.ScoreResult;

import java.util.List;

/**
 * Abstract interface for AI/LLM API providers.
 */
public interface AiApiClient {

    /**
     * Score a document on multiple dimensions.
     */
    ScoreResult score(String content);

    /**
     * Extract entities from text.
     */
    ExtractionResult extractEntities(String content);

    /**
     * Extract entities from text with few-shot examples.
     */
    ExtractionResult extractEntities(String content, List<ExampleData> examples);

    /**
     * Extract concepts from text.
     */
    ExtractionResult extractConcepts(String content);

    /**
     * Extract concepts from text with few-shot examples.
     */
    ExtractionResult extractConcepts(String content, List<ExampleData> examples);

    /**
     * Chat with the AI model.
     */
    String chat(String systemPrompt, String userMessage);

    /**
     * Check if the AI API service is available.
     */
    boolean isAvailable();

    /**
     * Extract entities with a specific temperature (single-pass).
     */
    ExtractionResult extractEntitiesWithTemperature(String content, double temperature);

    /**
     * Extract concepts with a specific temperature (single-pass).
     */
    ExtractionResult extractConceptsWithTemperature(String content, double temperature);
}
