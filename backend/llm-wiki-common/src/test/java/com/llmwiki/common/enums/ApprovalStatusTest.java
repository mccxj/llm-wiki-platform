package com.llmwiki.common.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApprovalStatusTest {

    @Test
    void shouldHaveAllRequiredStatuses() {
        assertNotNull(ApprovalStatus.PENDING);
        assertNotNull(ApprovalStatus.APPROVED);
        assertNotNull(ApprovalStatus.REJECTED);
    }

    @Test
    void shouldHaveExactly3Statuses() {
        assertEquals(3, ApprovalStatus.values().length);
    }
}

class NodeTypeTest {

    @Test
    void shouldHaveRequiredTypes() {
        assertNotNull(NodeType.ENTITY);
        assertNotNull(NodeType.CONCEPT);
        assertNotNull(NodeType.COMPARISON);
        assertNotNull(NodeType.RAW_SOURCE);
        assertNotNull(NodeType.QUERY);
    }

    @Test
    void shouldHaveExactly5Types() {
        assertEquals(5, NodeType.values().length);
    }
}

class StepStatusTest {

    @Test
    void shouldHaveAllRequiredStatuses() {
        assertNotNull(StepStatus.SUCCESS);
        assertNotNull(StepStatus.FAILED);
        assertNotNull(StepStatus.SKIPPED);
    }
}

class SyncStatusTest {

    @Test
    void shouldHaveAllRequiredStatuses() {
        assertNotNull(SyncStatus.SUCCESS);
        assertNotNull(SyncStatus.FAILED);
        assertNotNull(SyncStatus.RUNNING);
    }
}

class ConfidenceLevelTest {

    @Test
    void shouldHaveAllLevels() {
        assertNotNull(ConfidenceLevel.HIGH);
        assertNotNull(ConfidenceLevel.MEDIUM);
        assertNotNull(ConfidenceLevel.LOW);
    }

    @Test
    void shouldHaveExactly3Levels() {
        assertEquals(3, ConfidenceLevel.values().length);
    }
}
