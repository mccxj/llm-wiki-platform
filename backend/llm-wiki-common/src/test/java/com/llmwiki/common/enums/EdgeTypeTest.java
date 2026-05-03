package com.llmwiki.common.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EdgeTypeTest {

    @Test
    void shouldHaveAllRequiredTypes() {
        assertNotNull(EdgeType.RELATED_TO);
        assertNotNull(EdgeType.PART_OF);
        assertNotNull(EdgeType.DERIVED_FROM);
        assertNotNull(EdgeType.CONTRADICTS);
        assertNotNull(EdgeType.SUPERSEDES);
        assertNotNull(EdgeType.MENTIONS);
        assertNotNull(EdgeType.IS_A);
        assertNotNull(EdgeType.EXTENDS);
        assertNotNull(EdgeType.IMPLEMENTS);
        assertNotNull(EdgeType.DEPENDS_ON);
        assertNotNull(EdgeType.USED_BY);
        assertNotNull(EdgeType.CREATED_BY);
        assertNotNull(EdgeType.COMPETES_WITH);
        assertNotNull(EdgeType.SIMILAR_TO);
    }

    @Test
    void shouldHaveExactly14Types() {
        assertEquals(14, EdgeType.values().length);
    }

    @Test
    void shouldMapFromString() {
        assertEquals(EdgeType.DEPENDS_ON, EdgeType.valueOf("DEPENDS_ON"));
        assertEquals(EdgeType.IS_A, EdgeType.valueOf("IS_A"));
        assertEquals(EdgeType.CREATED_BY, EdgeType.valueOf("CREATED_BY"));
    }
}
