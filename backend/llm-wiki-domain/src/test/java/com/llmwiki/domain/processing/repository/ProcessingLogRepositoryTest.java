package com.llmwiki.domain.processing.repository;

import com.llmwiki.common.enums.StepStatus;
import com.llmwiki.domain.processing.entity.ProcessingLog;
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
class ProcessingLogRepositoryTest {

    @Autowired
    ProcessingLogRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveAndFindById() {
        ProcessingLog log = ProcessingLog.builder()
                .rawDocumentId(UUID.randomUUID())
                .step("SCORE")
                .status(StepStatus.SUCCESS)
                .detail("Score: 8.5")
                .build();

        ProcessingLog saved = repository.save(log);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        ProcessingLog found = repository.findById(saved.getId()).orElseThrow();
        assertEquals("SCORE", found.getStep());
        assertEquals(StepStatus.SUCCESS, found.getStatus());
        assertEquals("Score: 8.5", found.getDetail());
    }

    @Test
    void shouldFindByRawDocumentId() {
        UUID docId = UUID.randomUUID();
        repository.save(ProcessingLog.builder().rawDocumentId(docId).step("SCORE").status(StepStatus.SUCCESS).build());
        repository.save(ProcessingLog.builder().rawDocumentId(docId).step("ENTITY_EXTRACTION").status(StepStatus.SUCCESS).build());
        repository.save(ProcessingLog.builder().rawDocumentId(UUID.randomUUID()).step("SCORE").status(StepStatus.SUCCESS).build());

        List<ProcessingLog> results = repository.findByRawDocumentId(docId);
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(l -> l.getRawDocumentId().equals(docId)));
    }

    @Test
    void shouldReturnEmptyListForUnknownDocumentId() {
        List<ProcessingLog> results = repository.findByRawDocumentId(UUID.randomUUID());
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldTrackMultipleSteps() {
        UUID docId = UUID.randomUUID();
        repository.save(ProcessingLog.builder().rawDocumentId(docId).step("SCORE").status(StepStatus.SUCCESS).build());
        repository.save(ProcessingLog.builder().rawDocumentId(docId).step("ENTITY_EXTRACTION").status(StepStatus.SUCCESS).build());
        repository.save(ProcessingLog.builder().rawDocumentId(docId).step("CONCEPT_EXTRACTION").status(StepStatus.FAILED).build());
        repository.save(ProcessingLog.builder().rawDocumentId(docId).step("PAGE_GENERATION").status(StepStatus.SKIPPED).build());

        List<ProcessingLog> results = repository.findByRawDocumentId(docId);
        assertEquals(4, results.size());
    }

    @Test
    void shouldSupportAllStepStatuses() {
        UUID docId = UUID.randomUUID();
        repository.save(ProcessingLog.builder().rawDocumentId(docId).step("STEP1").status(StepStatus.SUCCESS).build());
        repository.save(ProcessingLog.builder().rawDocumentId(docId).step("STEP2").status(StepStatus.FAILED).build());
        repository.save(ProcessingLog.builder().rawDocumentId(docId).step("STEP3").status(StepStatus.SKIPPED).build());

        List<ProcessingLog> results = repository.findByRawDocumentId(docId);
        assertEquals(3, results.size());
    }
}
