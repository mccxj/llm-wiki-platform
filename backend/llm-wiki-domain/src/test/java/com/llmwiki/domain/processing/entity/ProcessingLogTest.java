package com.llmwiki.domain.processing.entity;

import com.llmwiki.common.enums.StepStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProcessingLogTest {

    @Test
    void shouldCreateWithBuilder() {
        UUID rawDocId = UUID.randomUUID();
        ProcessingLog log = ProcessingLog.builder()
                .id(UUID.randomUUID())
                .rawDocumentId(rawDocId)
                .step("SCORE")
                .status(StepStatus.SUCCESS)
                .detail("Score: 75.0")
                .build();

        assertNotNull(log.getId());
        assertEquals(rawDocId, log.getRawDocumentId());
        assertEquals("SCORE", log.getStep());
        assertEquals(StepStatus.SUCCESS, log.getStatus());
        assertEquals("Score: 75.0", log.getDetail());
    }

    @Test
    void shouldSupportAllStepStatuses() {
        ProcessingLog success = ProcessingLog.builder()
                .rawDocumentId(UUID.randomUUID())
                .step("ENTITY_EXTRACTION")
                .status(StepStatus.SUCCESS)
                .build();
        ProcessingLog failed = ProcessingLog.builder()
                .rawDocumentId(UUID.randomUUID())
                .step("SCORE")
                .status(StepStatus.FAILED)
                .build();
        ProcessingLog skipped = ProcessingLog.builder()
                .rawDocumentId(UUID.randomUUID())
                .step("CROSS_LINKING")
                .status(StepStatus.SKIPPED)
                .build();

        assertEquals(StepStatus.SUCCESS, success.getStatus());
        assertEquals(StepStatus.FAILED, failed.getStatus());
        assertEquals(StepStatus.SKIPPED, skipped.getStatus());
    }
}
