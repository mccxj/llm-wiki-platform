package com.llmwiki.domain.sync.repository;

import com.llmwiki.common.enums.SyncStatus;
import com.llmwiki.domain.sync.entity.SyncLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SyncLogRepositoryTest {

    @Autowired
    SyncLogRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveAndFindById() {
        SyncLog log = SyncLog.builder()
                .sourceId(UUID.randomUUID())
                .startedAt(Instant.now())
                .status(SyncStatus.SUCCESS)
                .fetchedCount(10)
                .processedCount(8)
                .skippedCount(1)
                .failedCount(1)
                .build();

        SyncLog saved = repository.save(log);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        SyncLog found = repository.findById(saved.getId()).orElseThrow();
        assertEquals(10, found.getFetchedCount());
        assertEquals(8, found.getProcessedCount());
        assertEquals(1, found.getSkippedCount());
        assertEquals(1, found.getFailedCount());
        assertEquals(SyncStatus.SUCCESS, found.getStatus());
    }

    @Test
    void shouldDefaultStatusToRunning() {
        SyncLog log = SyncLog.builder()
                .sourceId(UUID.randomUUID())
                .startedAt(Instant.now())
                .build();

        SyncLog saved = repository.save(log);
        assertEquals(SyncStatus.RUNNING, saved.getStatus());
    }

    @Test
    void shouldDefaultCountsToZero() {
        SyncLog log = SyncLog.builder()
                .sourceId(UUID.randomUUID())
                .startedAt(Instant.now())
                .build();

        SyncLog saved = repository.save(log);
        assertEquals(0, saved.getFetchedCount());
        assertEquals(0, saved.getProcessedCount());
        assertEquals(0, saved.getSkippedCount());
        assertEquals(0, saved.getFailedCount());
    }

    @Test
    void shouldStoreErrorMessage() {
        SyncLog log = SyncLog.builder()
                .sourceId(UUID.randomUUID())
                .startedAt(Instant.now())
                .status(SyncStatus.FAILED)
                .errorMessage("Connection timeout")
                .build();

        SyncLog saved = repository.save(log);
        SyncLog found = repository.findById(saved.getId()).orElseThrow();
        assertEquals("Connection timeout", found.getErrorMessage());
        assertEquals(SyncStatus.FAILED, found.getStatus());
    }

    @Test
    void shouldFindAll() {
        repository.save(SyncLog.builder()
                .sourceId(UUID.randomUUID()).startedAt(Instant.now()).build());
        repository.save(SyncLog.builder()
                .sourceId(UUID.randomUUID()).startedAt(Instant.now()).build());

        List<SyncLog> all = repository.findAll();
        assertEquals(2, all.size());
    }

    @Test
    void shouldStoreFinishedAt() {
        SyncLog log = SyncLog.builder()
                .sourceId(UUID.randomUUID())
                .startedAt(Instant.now())
                .finishedAt(Instant.now().plusSeconds(60))
                .status(SyncStatus.SUCCESS)
                .build();

        SyncLog saved = repository.save(log);
        assertNotNull(saved.getFinishedAt());
    }
}
