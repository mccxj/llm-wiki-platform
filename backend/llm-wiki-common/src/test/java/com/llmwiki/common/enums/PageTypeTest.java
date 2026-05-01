package com.llmwiki.common.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PageTypeTest {

    @Test
    void shouldHaveAllRequiredTypes() {
        assertNotNull(PageType.ENTITY);
        assertNotNull(PageType.CONCEPT);
        assertNotNull(PageType.COMPARISON);
        assertNotNull(PageType.QUERY);
        assertNotNull(PageType.RAW_SOURCE);
    }

    @Test
    void shouldHaveExactly5Types() {
        assertEquals(5, PageType.values().length);
    }
}
