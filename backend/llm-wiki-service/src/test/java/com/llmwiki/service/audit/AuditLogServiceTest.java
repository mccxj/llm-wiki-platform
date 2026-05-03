package com.llmwiki.service.audit;

import com.llmwiki.domain.audit.entity.AuditLog;
import com.llmwiki.domain.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepo;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    void log_shouldSaveAuditLog() {
        UUID entityId = UUID.randomUUID();
        auditLogService.log("CREATE", "Page", entityId, "admin", "Created page");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepo).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("CREATE", saved.getAction());
        assertEquals("Page", saved.getEntityType());
        assertEquals(entityId, saved.getEntityId());
        assertEquals("admin", saved.getOperator());
        assertEquals("Created page", saved.getDetail());
    }

    @Test
    void logChange_shouldSaveAuditLogWithBeforeAndAfter() {
        UUID entityId = UUID.randomUUID();
        auditLogService.logChange("UPDATE", "Page", entityId, "admin",
                "old title", "new title", "Title changed");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepo).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("UPDATE", saved.getAction());
        assertEquals("old title", saved.getBeforeValue());
        assertEquals("new title", saved.getAfterValue());
        assertEquals("Title changed", saved.getDetail());
    }

    @Test
    void getEntityHistory_shouldReturnLogsForEntity() {
        UUID entityId = UUID.randomUUID();
        AuditLog log1 = AuditLog.builder().id(UUID.randomUUID()).action("CREATE").build();
        AuditLog log2 = AuditLog.builder().id(UUID.randomUUID()).action("UPDATE").build();

        when(auditLogRepo.findByEntityTypeAndEntityId("Page", entityId))
                .thenReturn(List.of(log1, log2));

        List<AuditLog> result = auditLogService.getEntityHistory("Page", entityId);

        assertEquals(2, result.size());
        verify(auditLogRepo).findByEntityTypeAndEntityId("Page", entityId);
    }

    @Test
    void getRecentLogs_shouldReturnTop100() {
        List<AuditLog> logs = List.of(
                AuditLog.builder().id(UUID.randomUUID()).build(),
                AuditLog.builder().id(UUID.randomUUID()).build());
        when(auditLogRepo.findTop100ByOrderByCreatedAtDesc()).thenReturn(logs);

        List<AuditLog> result = auditLogService.getRecentLogs();

        assertEquals(2, result.size());
        verify(auditLogRepo).findTop100ByOrderByCreatedAtDesc();
    }
}
