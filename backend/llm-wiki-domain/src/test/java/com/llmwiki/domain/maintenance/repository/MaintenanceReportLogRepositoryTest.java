package com.llmwiki.domain.maintenance.repository;

import com.llmwiki.domain.maintenance.entity.MaintenanceReportLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class MaintenanceReportLogRepositoryTest {

    @Autowired
    MaintenanceReportLogRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveAndFindById() {
        MaintenanceReportLog log = MaintenanceReportLog.builder()
                .taskType("DEDUPLICATION")
                .result("{\"duplicates\":5}")
                .status("COMPLETED")
                .build();

        MaintenanceReportLog saved = repository.save(log);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        MaintenanceReportLog found = repository.findById(saved.getId()).orElseThrow();
        assertEquals("DEDUPLICATION", found.getTaskType());
        assertEquals("{\"duplicates\":5}", found.getResult());
        assertEquals("COMPLETED", found.getStatus());
    }

    @Test
    void shouldFindTop10ByTaskTypeOrderByCreatedAtDesc() {
        for (int i = 0; i < 15; i++) {
            repository.save(MaintenanceReportLog.builder()
                    .taskType("DEDUPLICATION")
                    .result("result-" + i)
                    .build());
        }
        for (int i = 0; i < 5; i++) {
            repository.save(MaintenanceReportLog.builder()
                    .taskType("ORPHAN_CLEANUP")
                    .result("result-" + i)
                    .build());
        }

        List<MaintenanceReportLog> dedupLogs = repository.findTop10ByTaskTypeOrderByCreatedAtDesc("DEDUPLICATION");
        assertEquals(10, dedupLogs.size());

        List<MaintenanceReportLog> orphanLogs = repository.findTop10ByTaskTypeOrderByCreatedAtDesc("ORPHAN_CLEANUP");
        assertEquals(5, orphanLogs.size());
    }

    @Test
    void shouldReturnOnlyMatchingTaskType() {
        repository.save(MaintenanceReportLog.builder().taskType("TYPE_A").result("r1").build());
        repository.save(MaintenanceReportLog.builder().taskType("TYPE_B").result("r2").build());
        repository.save(MaintenanceReportLog.builder().taskType("TYPE_A").result("r3").build());

        List<MaintenanceReportLog> results = repository.findTop10ByTaskTypeOrderByCreatedAtDesc("TYPE_A");
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(l -> "TYPE_A".equals(l.getTaskType())));
    }

    @Test
    void shouldDefaultStatusToCompleted() {
        MaintenanceReportLog log = MaintenanceReportLog.builder()
                .taskType("TEST")
                .result("result")
                .build();

        MaintenanceReportLog saved = repository.save(log);
        assertEquals("COMPLETED", saved.getStatus());
    }

    @Test
    void shouldReturnEmptyForUnknownTaskType() {
        List<MaintenanceReportLog> results = repository.findTop10ByTaskTypeOrderByCreatedAtDesc("NONEXISTENT");
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldFindAll() {
        repository.save(MaintenanceReportLog.builder().taskType("T1").result("r1").build());
        repository.save(MaintenanceReportLog.builder().taskType("T2").result("r2").build());

        List<MaintenanceReportLog> all = repository.findAll();
        assertEquals(2, all.size());
    }
}
