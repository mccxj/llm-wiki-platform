package com.llmwiki.domain.maintenance.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class MaintenanceReportLogTest {

    @Test
    void shouldCreateWithBuilder() {
        MaintenanceReportLog log = MaintenanceReportLog.builder()
                .id(UUID.randomUUID())
                .taskType("ORPHAN_CHECK")
                .result("{\"orphans\": 5}")
                .status("COMPLETED")
                .build();

        assertNotNull(log.getId());
        assertEquals("ORPHAN_CHECK", log.getTaskType());
        assertEquals("{\"orphans\": 5}", log.getResult());
        assertEquals("COMPLETED", log.getStatus());
    }

    @Test
    void shouldHaveDefaultStatus() {
        MaintenanceReportLog log = MaintenanceReportLog.builder()
                .taskType("DUPLICATE_CHECK")
                .build();

        assertEquals("COMPLETED", log.getStatus());
    }

    @Test
    void shouldSetCreatedAtViaPrePersist() {
        MaintenanceReportLog log = MaintenanceReportLog.builder()
                .taskType("ORPHAN_CHECK")
                .build();

        assertNull(log.getCreatedAt());
        log.onCreate();
        assertNotNull(log.getCreatedAt());
        assertFalse(log.getCreatedAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void shouldSupportNoArgsConstructor() {
        MaintenanceReportLog log = new MaintenanceReportLog();
        log.setTaskType("STALE_CHECK");
        log.setResult("done");
        log.setStatus("FAILED");

        assertEquals("STALE_CHECK", log.getTaskType());
        assertEquals("done", log.getResult());
        assertEquals("FAILED", log.getStatus());
    }
}
