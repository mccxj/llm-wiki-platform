package com.llmwiki.domain.graph.repository;

import com.llmwiki.common.enums.NodeType;
import com.llmwiki.domain.graph.entity.KgNode;
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
class KgNodeRepositoryTest {

    @Autowired
    KgNodeRepository repository;

    @Autowired
    KgEdgeRepository edgeRepository;

    @BeforeEach
    void setUp() {
        edgeRepository.deleteAll();
        repository.deleteAll();
    }

    @Test
    void shouldSaveAndFindById() {
        KgNode node = KgNode.builder()
                .name("Java")
                .nodeType(NodeType.ENTITY)
                .description("A programming language")
                .build();

        KgNode saved = repository.save(node);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        KgNode found = repository.findById(saved.getId()).orElseThrow();
        assertEquals("Java", found.getName());
        assertEquals(NodeType.ENTITY, found.getNodeType());
        assertEquals("A programming language", found.getDescription());
    }

    @Test
    void shouldFindByNameAndNodeType() {
        repository.save(KgNode.builder().name("Java").nodeType(NodeType.ENTITY).build());
        repository.save(KgNode.builder().name("Java").nodeType(NodeType.CONCEPT).build());

        Optional<KgNode> entity = repository.findByNameAndNodeType("Java", NodeType.ENTITY);
        assertTrue(entity.isPresent());
        assertEquals(NodeType.ENTITY, entity.get().getNodeType());

        Optional<KgNode> concept = repository.findByNameAndNodeType("Java", NodeType.CONCEPT);
        assertTrue(concept.isPresent());
        assertEquals(NodeType.CONCEPT, concept.get().getNodeType());

        Optional<KgNode> missing = repository.findByNameAndNodeType("Python", NodeType.ENTITY);
        assertFalse(missing.isPresent());
    }

    @Test
    void shouldFindByNameIgnoreCaseAndNodeType() {
        repository.save(KgNode.builder().name("Java").nodeType(NodeType.ENTITY).build());

        Optional<KgNode> found = repository.findByNameIgnoreCaseAndNodeType("java", NodeType.ENTITY);
        assertTrue(found.isPresent());
        assertEquals("Java", found.get().getName());

        Optional<KgNode> upper = repository.findByNameIgnoreCaseAndNodeType("JAVA", NodeType.ENTITY);
        assertTrue(upper.isPresent());
    }

    @Test
    void shouldFindByNameContaining() {
        repository.save(KgNode.builder().name("Java").nodeType(NodeType.ENTITY).build());
        repository.save(KgNode.builder().name("JavaScript").nodeType(NodeType.ENTITY).build());
        repository.save(KgNode.builder().name("Python").nodeType(NodeType.ENTITY).build());

        List<KgNode> results = repository.findByNameContaining("Java");
        assertEquals(2, results.size());
    }

    @Test
    void shouldCountByNodeType() {
        repository.save(KgNode.builder().name("Java").nodeType(NodeType.ENTITY).build());
        repository.save(KgNode.builder().name("Python").nodeType(NodeType.ENTITY).build());
        repository.save(KgNode.builder().name("OOP").nodeType(NodeType.CONCEPT).build());

        assertEquals(2, repository.countByNodeType(NodeType.ENTITY));
        assertEquals(1, repository.countByNodeType(NodeType.CONCEPT));
        assertEquals(0, repository.countByNodeType(NodeType.RAW_SOURCE));
    }

    @Test
    void shouldFindOrphanNodes() {
        KgNode connected = repository.save(
                KgNode.builder().name("Connected").nodeType(NodeType.ENTITY).build());
        KgNode orphan1 = repository.save(
                KgNode.builder().name("Orphan1").nodeType(NodeType.CONCEPT).build());
        KgNode orphan2 = repository.save(
                KgNode.builder().name("Orphan2").nodeType(NodeType.ENTITY).build());

        edgeRepository.save(com.llmwiki.domain.graph.entity.KgEdge.builder()
                .sourceNodeId(connected.getId())
                .targetNodeId(orphan1.getId())
                .edgeType(com.llmwiki.common.enums.EdgeType.RELATED_TO)
                .build());

        List<KgNode> orphans = repository.findOrphanNodes();
        assertEquals(1, orphans.size());
        assertEquals("Orphan2", orphans.get(0).getName());
    }

    @Test
    void shouldHandleEntitySubType() {
        KgNode node = KgNode.builder()
                .name("Java")
                .nodeType(NodeType.ENTITY)
                .entitySubType("PROGRAMMING_LANGUAGE")
                .build();

        KgNode saved = repository.save(node);
        KgNode found = repository.findById(saved.getId()).orElseThrow();
        assertEquals("PROGRAMMING_LANGUAGE", found.getEntitySubType());
    }

    @Test
    void shouldHandlePageId() {
        UUID pageId = UUID.randomUUID();
        KgNode node = KgNode.builder()
                .name("Java")
                .nodeType(NodeType.ENTITY)
                .pageId(pageId)
                .build();

        KgNode saved = repository.save(node);
        KgNode found = repository.findById(saved.getId()).orElseThrow();
        assertEquals(pageId, found.getPageId());
    }
}
