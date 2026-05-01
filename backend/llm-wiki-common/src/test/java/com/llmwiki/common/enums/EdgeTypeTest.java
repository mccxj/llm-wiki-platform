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
    }

    @Test
    void shouldHaveExactly6Types() {
        assertEquals(6, EdgeType.values().length);
    }
}
