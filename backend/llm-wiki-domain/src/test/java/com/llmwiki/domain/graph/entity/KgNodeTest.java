package com.llmwiki.domain.graph.entity;

import com.llmwiki.common.enums.NodeType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class KgNodeTest {

    @Test
    void shouldCreateWithBuilder() {
        KgNode node = KgNode.builder()
                .id(UUID.randomUUID())
                .name("Java")
                .nodeType(NodeType.ENTITY)
                .description("Programming language")
                .build();

        assertNotNull(node.getId());
        assertEquals("Java", node.getName());
        assertEquals(NodeType.ENTITY, node.getNodeType());
        assertEquals("Programming language", node.getDescription());
        assertNull(node.getPageId());
    }

    @Test
    void shouldSetPageId() {
        UUID pageId = UUID.randomUUID();
        KgNode node = KgNode.builder()
                .name("OOP")
                .nodeType(NodeType.CONCEPT)
                .pageId(pageId)
                .build();

        assertEquals(pageId, node.getPageId());
    }

    @Test
    void shouldSupportBothNodeTypes() {
        KgNode entity = KgNode.builder()
                .name("Java").nodeType(NodeType.ENTITY).build();
        KgNode concept = KgNode.builder()
                .name("OOP").nodeType(NodeType.CONCEPT).build();

        assertEquals(NodeType.ENTITY, entity.getNodeType());
        assertEquals(NodeType.CONCEPT, concept.getNodeType());
    }
}
