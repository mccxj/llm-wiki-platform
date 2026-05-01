package com.llmwiki.adapter.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiEmbeddingClientTest {

    OpenAiEmbeddingClient client;

    @BeforeEach
    void setUp() {
        client = new OpenAiEmbeddingClient("http://localhost:9999", "test-key", "test-model", 1536);
    }

    @Test
    void shouldCreateInstance() {
        assertNotNull(client);
        assertEquals(1536, client.getDimension());
    }

    @Test
    void embed_shouldReturnFallbackForUnreachableServer() {
        // When server is unreachable, embed returns zero-filled array (fallback)
        float[] result = client.embed("test");
        assertNotNull(result);
        assertEquals(1536, result.length);
        // All zeros since server is unreachable
        boolean allZero = true;
        for (float f : result) {
            if (f != 0.0f) {
                allZero = false;
                break;
            }
        }
        assertTrue(allZero);
    }
}
