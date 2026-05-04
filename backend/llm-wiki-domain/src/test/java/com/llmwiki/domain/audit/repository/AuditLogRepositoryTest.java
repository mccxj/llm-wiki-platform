package com.llmwiki.domain.audit.repository;

import com.llmwiki.domain.audit.entity.AuditLog;
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
class AuditLogRepositoryTest {

    @Autowired
    AuditLogRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveAndFindById() {
        AuditLog log = AuditLog.builder()
                .action("CREATE")
                .entityType("PAGE")
                .entityId(UUID.randomUUID())
                .operator("admin")
                .detail("{\"title\":\"Test\"}")
                .build();

        AuditLog saved = repository.save(log);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        AuditLog found = repository.findById(saved.getId()).orElseThrow();
        assertEquals("CREATE", found.getAction());
        assertEquals("PAGE", found.getEntityType());
        assertEquals("admin", found.getOperator());
    }

    @Test
    void shouldFindByEntityTypeAndEntityId() {
        UUID entityId = UUID.randomUUID();
        repository.save(AuditLog.builder()
                .action("CREATE").entityType("PAGE").entityId(entityId).operator("admin").build());
        repository.save(AuditLog.builder()
                .action("UPDATE").entityType("PAGE").entityId(entityId).operator("admin").build());
        repository.save(AuditLog.builder()
                .action("CREATE").entityType("NODE").entityId(UUID.randomUUID()).operator("admin").build());

        List<AuditLog> results = repository.findByEntityTypeAndEntityId("PAGE", entityId);
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(l ->
                "PAGE".equals(l.getEntityType()) && entityId.equals(l.getEntityId())));
    }

    @Test
    void shouldFindByOperatorOrderByCreatedAtDesc() {
        repository.save(AuditLog.builder()
                .action("CREATE").entityType("PAGE").operator("admin").build());
        repository.save(AuditLog.builder()
                .action("UPDATE").entityType("NODE").operator("admin").build());
        repository.save(AuditLog.builder()
                .action("DELETE").entityType("PAGE").operator("user1").build());

        List<AuditLog> results = repository.findByOperatorOrderByCreatedAtDesc("admin");
        assertEquals(2, results.size());
        assertTrue(results.get(0).getCreatedAt().isAfter(results.get(1).getCreatedAt())
                || results.get(0).getCreatedAt().equals(results.get(1).getCreatedAt()));
    }

    @Test
    void shouldFindTop100ByOrderByCreatedAtDesc() {
        for (int i = 0; i < 5; i++) {
            repository.save(AuditLog.builder()
                    .action("ACTION_" + i).entityType("PAGE").operator("admin").build());
        }

        List<AuditLog> results = repository.findTop100ByOrderByCreatedAtDesc();
        assertEquals(5, results.size());
        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(results.get(i).getCreatedAt().isAfter(results.get(i + 1).getCreatedAt())
                    || results.get(i).getCreatedAt().equals(results.get(i + 1).getCreatedAt()));
        }
    }

    @Test
    void shouldReturnEmptyListForUnknownOperator() {
        List<AuditLog> results = repository.findByOperatorOrderByCreatedAtDesc("nonexistent");
        assertTrue(results.isEmpty());
    }
}
