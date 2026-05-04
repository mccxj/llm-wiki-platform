package com.llmwiki.domain.approval.repository;

import com.llmwiki.domain.approval.entity.ApprovalQueue;
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
class ApprovalQueueRepositoryTest {

    @Autowired
    ApprovalQueueRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveAndFindById() {
        ApprovalQueue queue = ApprovalQueue.builder()
                .pageId(UUID.randomUUID())
                .action("CREATE")
                .status("PENDING")
                .build();

        ApprovalQueue saved = repository.save(queue);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals("PENDING", saved.getStatus());

        ApprovalQueue found = repository.findById(saved.getId()).orElseThrow();
        assertEquals("CREATE", found.getAction());
    }

    @Test
    void shouldFindByStatus() {
        UUID page1 = UUID.randomUUID();
        UUID page2 = UUID.randomUUID();
        repository.save(ApprovalQueue.builder().pageId(page1).action("CREATE").status("PENDING").build());
        repository.save(ApprovalQueue.builder().pageId(page2).action("UPDATE").status("PENDING").build());
        repository.save(ApprovalQueue.builder().pageId(UUID.randomUUID()).action("DELETE").status("APPROVED").build());

        List<ApprovalQueue> pending = repository.findByStatus("PENDING");
        assertEquals(2, pending.size());
        assertTrue(pending.stream().allMatch(q -> "PENDING".equals(q.getStatus())));

        List<ApprovalQueue> approved = repository.findByStatus("APPROVED");
        assertEquals(1, approved.size());
    }

    @Test
    void shouldFindByStatusOrderByCreatedAtAsc() {
        repository.save(ApprovalQueue.builder()
                .pageId(UUID.randomUUID()).action("CREATE").status("PENDING").build());
        repository.save(ApprovalQueue.builder()
                .pageId(UUID.randomUUID()).action("UPDATE").status("PENDING").build());

        List<ApprovalQueue> results = repository.findByStatusOrderByCreatedAtAsc("PENDING");
        assertEquals(2, results.size());
        assertTrue(results.get(0).getCreatedAt().isBefore(results.get(1).getCreatedAt())
                || results.get(0).getCreatedAt().equals(results.get(1).getCreatedAt()));
    }

    @Test
    void shouldCountByStatus() {
        repository.save(ApprovalQueue.builder()
                .pageId(UUID.randomUUID()).action("CREATE").status("PENDING").build());
        repository.save(ApprovalQueue.builder()
                .pageId(UUID.randomUUID()).action("UPDATE").status("PENDING").build());
        repository.save(ApprovalQueue.builder()
                .pageId(UUID.randomUUID()).action("DELETE").status("APPROVED").build());

        assertEquals(2, repository.countByStatus("PENDING"));
        assertEquals(1, repository.countByStatus("APPROVED"));
        assertEquals(0, repository.countByStatus("REJECTED"));
    }

    @Test
    void shouldFindByPageId() {
        UUID pageId = UUID.randomUUID();
        repository.save(ApprovalQueue.builder()
                .pageId(pageId).action("CREATE").status("PENDING").build());
        repository.save(ApprovalQueue.builder()
                .pageId(pageId).action("UPDATE").status("APPROVED").build());
        repository.save(ApprovalQueue.builder()
                .pageId(UUID.randomUUID()).action("DELETE").status("PENDING").build());

        List<ApprovalQueue> results = repository.findByPageId(pageId);
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(q -> q.getPageId().equals(pageId)));
    }

    @Test
    void shouldStoreEntityTypeAndValues() {
        ApprovalQueue queue = ApprovalQueue.builder()
                .pageId(UUID.randomUUID())
                .action("UPDATE")
                .entityType("PAGE")
                .beforeValue("{\"title\":\"Old\"}")
                .afterValue("{\"title\":\"New\"}")
                .summary("Title changed")
                .build();

        ApprovalQueue saved = repository.save(queue);
        ApprovalQueue found = repository.findById(saved.getId()).orElseThrow();
        assertEquals("PAGE", found.getEntityType());
        assertEquals("{\"title\":\"Old\"}", found.getBeforeValue());
        assertEquals("{\"title\":\"New\"}", found.getAfterValue());
        assertEquals("Title changed", found.getSummary());
    }
}
