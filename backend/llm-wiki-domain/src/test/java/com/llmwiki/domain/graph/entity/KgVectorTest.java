package com.llmwiki.domain.graph.entity;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class KgVectorTest {

    @Test
    void shouldCreateWithBuilder() {
        UUID nodeId = UUID.randomUUID();
        float[] vector = {0.1f, 0.2f, 0.3f};
        KgVector kgVector = KgVector.builder()
                .nodeId(nodeId)
                .vector(vector)
                .model("text-embedding-ada-002")
                .build();

        assertEquals(nodeId, kgVector.getNodeId());
        assertArrayEquals(vector, kgVector.getVector());
        assertEquals("text-embedding-ada-002", kgVector.getModel());
    }

    @Test
    void nodeIdIsThePrimaryKey() {
        UUID nodeId = UUID.randomUUID();
        KgVector kgVector = KgVector.builder()
                .nodeId(nodeId)
                .vector(new float[]{0.5f})
                .build();

        assertEquals(nodeId, kgVector.getNodeId());
    }

    @Test
    void shouldSupportNoArgsConstructor() {
        KgVector kgVector = new KgVector();
        kgVector.setNodeId(UUID.randomUUID());
        kgVector.setVector(new float[]{0.1f, 0.2f});
        kgVector.setModel("test-model");

        assertNotNull(kgVector.getNodeId());
        assertArrayEquals(new float[]{0.1f, 0.2f}, kgVector.getVector());
        assertEquals("test-model", kgVector.getModel());
    }
}
