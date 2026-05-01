package com.llmwiki.domain.approval.entity;

import com.llmwiki.common.enums.ApprovalStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ApprovalQueueTest {

    @Test
    void shouldCreateWithBuilder() {
        UUID pageId = UUID.randomUUID();
        ApprovalQueue approval = ApprovalQueue.builder()
                .id(UUID.randomUUID())
                .pageId(pageId)
                .action("CREATE")
                .status(ApprovalStatus.PENDING.name())
                .comment("New page submitted")
                .build();

        assertNotNull(approval.getId());
        assertEquals(pageId, approval.getPageId());
        assertEquals("CREATE", approval.getAction());
        assertEquals(ApprovalStatus.PENDING.name(), approval.getStatus());
        assertEquals("New page submitted", approval.getComment());
        assertNull(approval.getReviewerId());
    }

    @Test
    void shouldUpdateStatus() {
        ApprovalQueue approval = ApprovalQueue.builder()
                .id(UUID.randomUUID())
                .pageId(UUID.randomUUID())
                .action("CREATE")
                .status(ApprovalStatus.PENDING.name())
                .build();

        approval.setStatus(ApprovalStatus.APPROVED.name());
        approval.setReviewerId("admin");
        approval.setReviewedAt(Instant.now());
        approval.setComment("Approved");

        assertEquals(ApprovalStatus.APPROVED.name(), approval.getStatus());
        assertEquals("admin", approval.getReviewerId());
        assertNotNull(approval.getReviewedAt());
    }

    @Test
    void shouldSupportAllActions() {
        ApprovalQueue create = ApprovalQueue.builder()
                .pageId(UUID.randomUUID()).action("CREATE").status("PENDING").build();
        ApprovalQueue update = ApprovalQueue.builder()
                .pageId(UUID.randomUUID()).action("UPDATE").status("PENDING").build();
        ApprovalQueue delete = ApprovalQueue.builder()
                .pageId(UUID.randomUUID()).action("DELETE").status("PENDING").build();

        assertEquals("CREATE", create.getAction());
        assertEquals("UPDATE", update.getAction());
        assertEquals("DELETE", delete.getAction());
    }

    @Test
    void shouldHaveDefaultStatus() {
        ApprovalQueue approval = ApprovalQueue.builder()
                .pageId(UUID.randomUUID())
                .action("CREATE")
                .build();

        assertEquals(ApprovalStatus.PENDING.name(), approval.getStatus());
    }
}
