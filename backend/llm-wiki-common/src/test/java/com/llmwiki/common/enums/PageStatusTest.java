package com.llmwiki.common.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PageStatusTest {

    @Test
    void shouldHaveAllRequiredStatuses() {
        assertNotNull(PageStatus.PENDING_APPROVAL);
        assertNotNull(PageStatus.APPROVED);
        assertNotNull(PageStatus.REJECTED);
        assertNotNull(PageStatus.ARCHIVED);
    }

    @Test
    void shouldHaveExactly4Statuses() {
        assertEquals(4, PageStatus.values().length);
    }
}
