package com.llmwiki.domain.sync.entity;

import com.llmwiki.common.enums.SyncStatus;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class SyncLogTest {

    @Test
    void shouldCreateWithBuilder() {
        UUID sourceId = UUID.randomUUID();
        Instant startedAt = Instant.now();
        SyncLog log = SyncLog.builder()
                .id(UUID.randomUUID())
                .sourceId(sourceId)
                .startedAt(startedAt)
                .fetchedCount(10)
                .processedCount(8)
                .skippedCount(1)
                .failedCount(1)
                .status(SyncStatus.SUCCESS)
                .build();

        assertNotNull(log.getId());
        assertEquals(sourceId, log.getSourceId());
        assertEquals(startedAt, log.getStartedAt());
        assertEquals(10, log.getFetchedCount());
        assertEquals(8, log.getProcessedCount());
        assertEquals(1, log.getSkippedCount());
        assertEquals(1, log.getFailedCount());
        assertEquals(SyncStatus.SUCCESS, log.getStatus());
    }

    @Test
    void shouldHaveDefaultValues() {
        SyncLog log = SyncLog.builder()
                .sourceId(UUID.randomUUID())
                .startedAt(Instant.now())
                .build();

        assertEquals(0, log.getFetchedCount());
        assertEquals(0, log.getProcessedCount());
        assertEquals(0, log.getSkippedCount());
        assertEquals(0, log.getFailedCount());
        assertEquals(SyncStatus.RUNNING, log.getStatus());
    }

    @Test
    void shouldSupportAllStatuses() {
        SyncLog running = SyncLog.builder()
                .sourceId(UUID.randomUUID())
                .startedAt(Instant.now())
                .status(SyncStatus.RUNNING)
                .build();
        SyncLog success = SyncLog.builder()
                .sourceId(UUID.randomUUID())
                .startedAt(Instant.now())
                .status(SyncStatus.SUCCESS)
                .build();
        SyncLog partial = SyncLog.builder()
                .sourceId(UUID.randomUUID())
                .startedAt(Instant.now())
                .status(SyncStatus.PARTIAL)
                .build();
        SyncLog failed = SyncLog.builder()
                .sourceId(UUID.randomUUID())
                .startedAt(Instant.now())
                .status(SyncStatus.FAILED)
                .build();

        assertEquals(SyncStatus.RUNNING, running.getStatus());
        assertEquals(SyncStatus.SUCCESS, success.getStatus());
        assertEquals(SyncStatus.PARTIAL, partial.getStatus());
        assertEquals(SyncStatus.FAILED, failed.getStatus());
    }

    @Test
    void shouldSetErrorMessage() {
        SyncLog log = SyncLog.builder()
                .sourceId(UUID.randomUUID())
                .startedAt(Instant.now())
                .build();

        log.setErrorMessage("Connection timeout");
        log.setStatus(SyncStatus.FAILED);
        log.setFinishedAt(Instant.now());

        assertEquals("Connection timeout", log.getErrorMessage());
        assertEquals(SyncStatus.FAILED, log.getStatus());
        assertNotNull(log.getFinishedAt());
    }
}
