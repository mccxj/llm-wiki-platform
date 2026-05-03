package com.llmwiki.domain.approval.entity;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ApprovalAuditTest {

    @Test
    void shouldCreateWithBuilder() {
        UUID approvalId = UUID.randomUUID();
        ApprovalAudit audit = ApprovalAudit.builder()
                .id(UUID.randomUUID())
                .approvalId(approvalId)
                .action("APPROVE")
                .reviewerId("admin")
                .comment("Looks good")
                .build();

        assertNotNull(audit.getId());
        assertEquals(approvalId, audit.getApprovalId());
        assertEquals("APPROVE", audit.getAction());
        assertEquals("admin", audit.getReviewerId());
        assertEquals("Looks good", audit.getComment());
    }

    @Test
    void shouldSupportAllActions() {
        ApprovalAudit approve = ApprovalAudit.builder()
                .approvalId(UUID.randomUUID())
                .action("APPROVE")
                .reviewerId("admin")
                .build();
        ApprovalAudit reject = ApprovalAudit.builder()
                .approvalId(UUID.randomUUID())
                .action("REJECT")
                .reviewerId("admin")
                .build();

        assertEquals("APPROVE", approve.getAction());
        assertEquals("REJECT", reject.getAction());
    }

    @Test
    void shouldSupportNoArgsConstructor() {
        ApprovalAudit audit = new ApprovalAudit();
        audit.setApprovalId(UUID.randomUUID());
        audit.setAction("APPROVE");
        audit.setReviewerId("reviewer1");
        audit.setComment("Approved");

        assertNotNull(audit.getApprovalId());
        assertEquals("APPROVE", audit.getAction());
        assertEquals("reviewer1", audit.getReviewerId());
        assertEquals("Approved", audit.getComment());
    }
}
