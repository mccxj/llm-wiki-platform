package com.llmwiki.domain.audit.entity;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AuditLogTest {

    @Test
    void shouldCreateWithBuilder() {
        UUID entityId = UUID.randomUUID();
        AuditLog log = AuditLog.builder()
                .id(UUID.randomUUID())
                .action("CREATE")
                .entityType("Page")
                .entityId(entityId)
                .operator("admin")
                .detail("{\"key\":\"value\"}")
                .beforeValue("old")
                .afterValue("new")
                .ipAddress("127.0.0.1")
                .build();

        assertNotNull(log.getId());
        assertEquals("CREATE", log.getAction());
        assertEquals("Page", log.getEntityType());
        assertEquals(entityId, log.getEntityId());
        assertEquals("admin", log.getOperator());
        assertEquals("{\"key\":\"value\"}", log.getDetail());
        assertEquals("old", log.getBeforeValue());
        assertEquals("new", log.getAfterValue());
        assertEquals("127.0.0.1", log.getIpAddress());
    }

    @Test
    void shouldSupportAllActions() {
        AuditLog create = AuditLog.builder().action("CREATE").entityType("Page").build();
        AuditLog update = AuditLog.builder().action("UPDATE").entityType("Page").build();
        AuditLog delete = AuditLog.builder().action("DELETE").entityType("Page").build();
        AuditLog approve = AuditLog.builder().action("APPROVE").entityType("Page").build();
        AuditLog reject = AuditLog.builder().action("REJECT").entityType("Page").build();

        assertEquals("CREATE", create.getAction());
        assertEquals("UPDATE", update.getAction());
        assertEquals("DELETE", delete.getAction());
        assertEquals("APPROVE", approve.getAction());
        assertEquals("REJECT", reject.getAction());
    }

    @Test
    void shouldSupportNoArgsConstructor() {
        AuditLog log = new AuditLog();
        log.setAction("LOGIN");
        log.setEntityType("User");
        log.setEntityId(UUID.randomUUID());
        log.setOperator("user1");

        assertEquals("LOGIN", log.getAction());
        assertEquals("User", log.getEntityType());
        assertNotNull(log.getEntityId());
        assertEquals("user1", log.getOperator());
    }
}
