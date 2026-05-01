package com.llmwiki.domain.pipeline.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeadLetterQueueTest {

    @Test
    void shouldCreateWithBuilder() {
        UUID rawDocId = UUID.randomUUID();
        DeadLetterQueue dlq = DeadLetterQueue.builder()
                .id(UUID.randomUUID())
                .rawDocumentId(rawDocId)
                .step("SCORE")
                .errorMessage("AI service unavailable")
                .payload("{\"title\":\"test\"}")
                .retryCount(0)
                .maxRetries(3)
                .status("PENDING")
                .build();

        assertNotNull(dlq.getId());
        assertEquals(rawDocId, dlq.getRawDocumentId());
        assertEquals("SCORE", dlq.getStep());
        assertEquals("AI service unavailable", dlq.getErrorMessage());
        assertEquals("{\"title\":\"test\"}", dlq.getPayload());
        assertEquals(0, dlq.getRetryCount());
        assertEquals(3, dlq.getMaxRetries());
        assertEquals("PENDING", dlq.getStatus());
    }

    @Test
    void shouldHaveDefaultStatus() {
        DeadLetterQueue dlq = DeadLetterQueue.builder()
                .rawDocumentId(UUID.randomUUID())
                .step("ENTITY_EXTRACTION")
                .build();

        assertEquals("PENDING", dlq.getStatus());
        assertEquals(0, dlq.getRetryCount());
        assertEquals(3, dlq.getMaxRetries());
    }

    @Test
    void shouldUpdateRetryCount() {
        DeadLetterQueue dlq = DeadLetterQueue.builder()
                .rawDocumentId(UUID.randomUUID())
                .step("SCORE")
                .build();

        dlq.setRetryCount(1);
        dlq.setErrorMessage("Timeout on retry 1");

        assertEquals(1, dlq.getRetryCount());
        assertEquals("Timeout on retry 1", dlq.getErrorMessage());
    }

    @Test
    void shouldMarkAsResolved() {
        DeadLetterQueue dlq = DeadLetterQueue.builder()
                .rawDocumentId(UUID.randomUUID())
                .step("SCORE")
                .build();

        dlq.setStatus("RESOLVED");
        dlq.setRetryCount(2);

        assertEquals("RESOLVED", dlq.getStatus());
        assertEquals(2, dlq.getRetryCount());
    }

    @Test
    void shouldSetTimestampsViaPrePersist() {
        DeadLetterQueue dlq = DeadLetterQueue.builder()
                .rawDocumentId(UUID.randomUUID())
                .step("SCORE")
                .build();

        assertNull(dlq.getCreatedAt());
        assertNull(dlq.getUpdatedAt());

        dlq.onCreate();

        assertNotNull(dlq.getCreatedAt());
        assertNotNull(dlq.getUpdatedAt());
        assertFalse(dlq.getCreatedAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void shouldUpdateTimestampViaPreUpdate() {
        DeadLetterQueue dlq = DeadLetterQueue.builder()
                .rawDocumentId(UUID.randomUUID())
                .step("SCORE")
                .build();

        dlq.onCreate();
        LocalDateTime originalUpdatedAt = dlq.getUpdatedAt();

        dlq.onUpdate();

        assertNotNull(dlq.getUpdatedAt());
        assertTrue(!dlq.getUpdatedAt().isBefore(originalUpdatedAt));
    }

    @Test
    void shouldSupportNoArgsConstructor() {
        DeadLetterQueue dlq = new DeadLetterQueue();
        dlq.setStep("PAGE_GENERATION");
        dlq.setErrorMessage("Page generation failed");

        assertEquals("PAGE_GENERATION", dlq.getStep());
        assertEquals("Page generation failed", dlq.getErrorMessage());
        assertEquals("PENDING", dlq.getStatus());
        assertEquals(0, dlq.getRetryCount());
        assertEquals(3, dlq.getMaxRetries());
    }

    @Test
    void shouldSetPayload() {
        DeadLetterQueue dlq = DeadLetterQueue.builder()
                .rawDocumentId(UUID.randomUUID())
                .step("SCORE")
                .build();

        dlq.setPayload("{\"docId\":\"123\",\"title\":\"test\"}");
        assertEquals("{\"docId\":\"123\",\"title\":\"test\"}", dlq.getPayload());
    }
}
