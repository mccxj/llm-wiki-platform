package com.llmwiki.domain.approval.repository;

import com.llmwiki.domain.approval.entity.ApprovalAudit;
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
class ApprovalAuditRepositoryTest {

    @Autowired
    ApprovalAuditRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveAndFindById() {
        ApprovalAudit audit = ApprovalAudit.builder()
                .approvalId(UUID.randomUUID())
                .action("APPROVE")
                .reviewerId("admin")
                .comment("Looks good")
                .build();

        ApprovalAudit saved = repository.save(audit);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        ApprovalAudit found = repository.findById(saved.getId()).orElseThrow();
        assertEquals("APPROVE", found.getAction());
        assertEquals("admin", found.getReviewerId());
        assertEquals("Looks good", found.getComment());
    }

    @Test
    void shouldFindByApprovalId() {
        UUID approvalId = UUID.randomUUID();
        repository.save(ApprovalAudit.builder()
                .approvalId(approvalId).action("APPROVE").reviewerId("admin").build());
        repository.save(ApprovalAudit.builder()
                .approvalId(approvalId).action("REJECT").reviewerId("admin2").build());
        repository.save(ApprovalAudit.builder()
                .approvalId(UUID.randomUUID()).action("APPROVE").reviewerId("admin").build());

        List<ApprovalAudit> results = repository.findByApprovalId(approvalId);
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(a -> a.getApprovalId().equals(approvalId)));
    }

    @Test
    void shouldReturnEmptyListForUnknownApprovalId() {
        List<ApprovalAudit> results = repository.findByApprovalId(UUID.randomUUID());
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldFindAll() {
        repository.save(ApprovalAudit.builder()
                .approvalId(UUID.randomUUID()).action("APPROVE").reviewerId("admin").build());
        repository.save(ApprovalAudit.builder()
                .approvalId(UUID.randomUUID()).action("REJECT").reviewerId("admin").build());

        List<ApprovalAudit> all = repository.findAll();
        assertEquals(2, all.size());
    }
}
