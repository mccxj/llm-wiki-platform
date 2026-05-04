package com.llmwiki.domain.graph.repository;

import com.llmwiki.domain.graph.entity.KgVector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class KgVectorRepositoryTest {

    @Autowired
    KgVectorRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveAndFindByNodeId() {
        UUID nodeId = UUID.randomUUID();
        float[] vector = {0.1f, 0.2f, 0.3f};
        KgVector kv = KgVector.builder()
                .nodeId(nodeId)
                .vector(vector)
                .model("text-embedding-ada-002")
                .build();

        KgVector saved = repository.save(kv);
        assertNotNull(saved.getCreatedAt());

        Optional<KgVector> found = repository.findByNodeId(nodeId);
        assertTrue(found.isPresent());
        assertArrayEquals(vector, found.get().getVector());
        assertEquals("text-embedding-ada-002", found.get().getModel());
    }

    @Test
    void shouldReturnEmptyForUnknownNodeId() {
        Optional<KgVector> found = repository.findByNodeId(UUID.randomUUID());
        assertFalse(found.isPresent());
    }

    @Test
    void shouldFindByNodeIdNot() {
        UUID nodeId1 = UUID.randomUUID();
        UUID nodeId2 = UUID.randomUUID();
        UUID nodeId3 = UUID.randomUUID();
        repository.save(KgVector.builder().nodeId(nodeId1).vector(new float[]{0.1f}).model("m").build());
        repository.save(KgVector.builder().nodeId(nodeId2).vector(new float[]{0.2f}).model("m").build());
        repository.save(KgVector.builder().nodeId(nodeId3).vector(new float[]{0.3f}).model("m").build());

        List<KgVector> results = repository.findByNodeIdNot(nodeId1);
        assertEquals(2, results.size());
        assertTrue(results.stream().noneMatch(v -> v.getNodeId().equals(nodeId1)));
    }

    @Test
    void shouldFindByNodeIdNotReturningAllWhenExcludedNotPresent() {
        UUID nodeId = UUID.randomUUID();
        repository.save(KgVector.builder().nodeId(nodeId).vector(new float[]{0.1f}).model("m").build());

        List<KgVector> results = repository.findByNodeIdNot(UUID.randomUUID());
        assertEquals(1, results.size());
    }

    @Test
    void shouldUpdateVector() {
        UUID nodeId = UUID.randomUUID();
        KgVector kv = KgVector.builder()
                .nodeId(nodeId)
                .vector(new float[]{0.1f, 0.2f})
                .model("old-model")
                .build();
        repository.save(kv);

        float[] newVector = {0.5f, 0.6f, 0.7f};
        kv.setVector(newVector);
        kv.setModel("new-model");
        repository.save(kv);

        KgVector found = repository.findByNodeId(nodeId).orElseThrow();
        assertArrayEquals(newVector, found.getVector());
        assertEquals("new-model", found.getModel());
    }

    @Test
    void shouldFindAll() {
        repository.save(KgVector.builder().nodeId(UUID.randomUUID()).vector(new float[]{0.1f}).model("m").build());
        repository.save(KgVector.builder().nodeId(UUID.randomUUID()).vector(new float[]{0.2f}).model("m").build());

        List<KgVector> all = repository.findAll();
        assertEquals(2, all.size());
    }
}
