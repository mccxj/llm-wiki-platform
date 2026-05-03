package com.llmwiki.common.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApprovalActionTest {

    @Test
    void shouldHaveAllRequiredActions() {
        assertNotNull(ApprovalAction.CREATE);
        assertNotNull(ApprovalAction.UPDATE);
        assertNotNull(ApprovalAction.DELETE);
    }

    @Test
    void shouldHaveExactly3Actions() {
        assertEquals(3, ApprovalAction.values().length);
    }

    @Test
    void shouldMapFromString() {
        assertEquals(ApprovalAction.CREATE, ApprovalAction.valueOf("CREATE"));
        assertEquals(ApprovalAction.UPDATE, ApprovalAction.valueOf("UPDATE"));
        assertEquals(ApprovalAction.DELETE, ApprovalAction.valueOf("DELETE"));
    }
}
