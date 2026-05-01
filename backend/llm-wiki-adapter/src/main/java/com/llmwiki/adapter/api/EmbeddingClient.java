package com.llmwiki.adapter.api;

/**
 * Abstract interface for text embedding providers.
 */
public interface EmbeddingClient {

    /**
     * Generate embedding vector for the given text.
     */
    float[] embed(String text);

    /**
     * Get the dimension of the embedding vector.
     */
    int getDimension();
}
