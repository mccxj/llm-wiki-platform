package com.llmwiki.domain.pipeline.repository;

import com.llmwiki.domain.pipeline.entity.DeadLetterQueue;
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
class DeadLetterQueueRepositoryTest {

    @Autowired
    DeadLetterQueueRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveAndFindById() {
        DeadLetterQueue dlq = DeadLetterQueue.builder()
                .rawDocumentId(UUID.randomUUID())
                .step("SCORE")
                .errorMessage("AI timeout")
                .build();

        DeadLetterQueue saved = repository.save(dlq);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        DeadLetterQueue found = repository.findById(saved.getId()).orElseThrow();
        assertEquals("SCORE", found.getStep());
        assertEquals("AI timeout", found.getErrorMessage());
        assertEquals("PENDING", found.getStatus());
    }

    @Test
    void shouldFindByStatus() {
        repository.save(DeadLetterQueue.builder()
                .rawDocumentId(UUID.randomUUID())
                .step("SCORE")
                .status("PENDING")
                .build());
        repository.save(DeadLetterQueue.builder()
                .rawDocumentId(UUID.randomUUID())
                .step("ENTITY_EXTRACTION")
                .status("PENDING")
                .build());
        repository.save(DeadLetterQueue.builder()
                .rawDocumentId(UUID.randomUUID())
                .step("SCORE")
                .status("RESOLVED")
                .build());

        List<DeadLetterQueue> pending = repository.findByStatus("PENDING");
        assertEquals(2, pending.size());
        assertTrue(pending.stream().allMatch(d -> "PENDING".equals(d.getStatus())));

        List<DeadLetterQueue> resolvedList = repository.findByStatus("RESOLVED");
        assertEquals(1, resolvedList.size());
    }

    @Test
    void shouldFindAll() {
        repository.save(DeadLetterQueue.builder()
                .rawDocumentId(UUID.randomUUID()).step("SCORE").build());
        repository.save(DeadLetterQueue.builder()
                .rawDocumentId(UUID.randomUUID()).step("PAGE_GENERATION").build());

        List<DeadLetterQueue> all = repository.findAll();
        assertEquals(2, all.size());
    }
}
