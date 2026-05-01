package com.llmwiki.adapter.api;

import com.llmwiki.adapter.dto.ScoreResult;
import com.llmwiki.adapter.dto.ExtractionResult;

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
     * Extract concepts from text.
     */
    ExtractionResult extractConcepts(String content);

    /**
     * Chat with the AI model.
     */
    String chat(String systemPrompt, String userMessage);

    /**
     * Check if the AI API service is available.
     */
    boolean isAvailable();
}
