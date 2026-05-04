package com.llmwiki.domain.graph.repository;

import com.llmwiki.common.enums.EdgeType;
import com.llmwiki.domain.graph.entity.KgEdge;
import com.llmwiki.domain.graph.entity.KgNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class KgEdgeRepositoryTest {

    @Autowired
    KgEdgeRepository repository;

    @Autowired
    KgNodeRepository nodeRepository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        nodeRepository.deleteAll();
    }

    @Test
    void shouldSaveAndFindById() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        KgEdge edge = KgEdge.builder()
                .sourceNodeId(sourceId)
                .targetNodeId(targetId)
                .edgeType(EdgeType.RELATED_TO)
                .weight(BigDecimal.valueOf(0.75))
                .build();

        KgEdge saved = repository.save(edge);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals(0, BigDecimal.valueOf(0.75).compareTo(saved.getWeight()));

        KgEdge found = repository.findById(saved.getId()).orElseThrow();
        assertEquals(sourceId, found.getSourceNodeId());
        assertEquals(targetId, found.getTargetNodeId());
        assertEquals(EdgeType.RELATED_TO, found.getEdgeType());
    }

    @Test
    void shouldFindBySourceNodeId() {
        UUID sourceId = UUID.randomUUID();
        UUID target1 = UUID.randomUUID();
        UUID target2 = UUID.randomUUID();
        repository.save(KgEdge.builder().sourceNodeId(sourceId).targetNodeId(target1).edgeType(EdgeType.RELATED_TO).build());
        repository.save(KgEdge.builder().sourceNodeId(sourceId).targetNodeId(target2).edgeType(EdgeType.IS_A).build());
        repository.save(KgEdge.builder().sourceNodeId(UUID.randomUUID()).targetNodeId(UUID.randomUUID()).edgeType(EdgeType.RELATED_TO).build());

        List<KgEdge> results = repository.findBySourceNodeId(sourceId);
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(e -> e.getSourceNodeId().equals(sourceId)));
    }

    @Test
    void shouldFindByTargetNodeId() {
        UUID targetId = UUID.randomUUID();
        repository.save(KgEdge.builder().sourceNodeId(UUID.randomUUID()).targetNodeId(targetId).edgeType(EdgeType.RELATED_TO).build());
        repository.save(KgEdge.builder().sourceNodeId(UUID.randomUUID()).targetNodeId(targetId).edgeType(EdgeType.PART_OF).build());
        repository.save(KgEdge.builder().sourceNodeId(UUID.randomUUID()).targetNodeId(UUID.randomUUID()).edgeType(EdgeType.RELATED_TO).build());

        List<KgEdge> results = repository.findByTargetNodeId(targetId);
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(e -> e.getTargetNodeId().equals(targetId)));
    }

    @Test
    void shouldCountByEdgeType() {
        repository.save(KgEdge.builder().sourceNodeId(UUID.randomUUID()).targetNodeId(UUID.randomUUID()).edgeType(EdgeType.RELATED_TO).build());
        repository.save(KgEdge.builder().sourceNodeId(UUID.randomUUID()).targetNodeId(UUID.randomUUID()).edgeType(EdgeType.RELATED_TO).build());
        repository.save(KgEdge.builder().sourceNodeId(UUID.randomUUID()).targetNodeId(UUID.randomUUID()).edgeType(EdgeType.IS_A).build());

        assertEquals(2, repository.countByEdgeType(EdgeType.RELATED_TO));
        assertEquals(1, repository.countByEdgeType(EdgeType.IS_A));
        assertEquals(0, repository.countByEdgeType(EdgeType.DEPENDS_ON));
    }

    @Test
    void shouldFindConnectedPageIds() {
        KgNode node1 = nodeRepository.save(KgNode.builder().name("N1").nodeType(com.llmwiki.common.enums.NodeType.ENTITY).build());
        KgNode node2 = nodeRepository.save(KgNode.builder().name("N2").nodeType(com.llmwiki.common.enums.NodeType.ENTITY).build());
        KgNode node3 = nodeRepository.save(KgNode.builder().name("N3").nodeType(com.llmwiki.common.enums.NodeType.ENTITY).build());

        UUID pageId1 = UUID.randomUUID();
        UUID pageId2 = UUID.randomUUID();
        node1.setPageId(pageId1);
        node2.setPageId(pageId2);
        nodeRepository.save(node1);
        nodeRepository.save(node2);

        repository.save(KgEdge.builder().sourceNodeId(node1.getId()).targetNodeId(node2.getId()).edgeType(EdgeType.RELATED_TO).build());
        repository.save(KgEdge.builder().sourceNodeId(node2.getId()).targetNodeId(node3.getId()).edgeType(EdgeType.RELATED_TO).build());

        List<UUID> connectedPageIds = repository.findConnectedPageIds();
        assertTrue(connectedPageIds.contains(pageId1));
        assertTrue(connectedPageIds.contains(pageId2));
    }

    @Test
    void shouldDefaultWeightToHalf() {
        KgEdge edge = KgEdge.builder()
                .sourceNodeId(UUID.randomUUID())
                .targetNodeId(UUID.randomUUID())
                .edgeType(EdgeType.RELATED_TO)
                .build();

        KgEdge saved = repository.save(edge);
        KgEdge found = repository.findById(saved.getId()).orElseThrow();
        assertEquals(0, BigDecimal.valueOf(0.50).compareTo(found.getWeight()));
    }
}
