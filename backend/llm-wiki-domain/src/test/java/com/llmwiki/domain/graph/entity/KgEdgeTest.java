package com.llmwiki.domain.graph.entity;

import com.llmwiki.common.enums.EdgeType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class KgEdgeTest {

    @Test
    void shouldCreateWithBuilder() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        KgEdge edge = KgEdge.builder()
                .id(UUID.randomUUID())
                .sourceNodeId(sourceId)
                .targetNodeId(targetId)
                .edgeType(EdgeType.RELATED_TO)
                .weight(BigDecimal.valueOf(0.75))
                .build();

        assertEquals(sourceId, edge.getSourceNodeId());
        assertEquals(targetId, edge.getTargetNodeId());
        assertEquals(EdgeType.RELATED_TO, edge.getEdgeType());
        assertEquals(BigDecimal.valueOf(0.75), edge.getWeight());
    }

    @Test
    void shouldSupportDifferentEdgeTypes() {
        KgEdge partOf = KgEdge.builder()
                .sourceNodeId(UUID.randomUUID())
                .targetNodeId(UUID.randomUUID())
                .edgeType(EdgeType.PART_OF)
                .weight(BigDecimal.valueOf(1.0))
                .build();

        assertEquals(EdgeType.PART_OF, partOf.getEdgeType());
    }

    @Test
    void shouldHaveDefaultWeight() {
        KgEdge edge = KgEdge.builder()
                .sourceNodeId(UUID.randomUUID())
                .targetNodeId(UUID.randomUUID())
                .edgeType(EdgeType.RELATED_TO)
                .build();

        assertEquals(BigDecimal.valueOf(0.50), edge.getWeight());
    }
}
