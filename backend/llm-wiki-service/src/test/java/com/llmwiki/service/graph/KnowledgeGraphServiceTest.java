package com.llmwiki.service.graph;

import com.llmwiki.common.enums.EdgeType;
import com.llmwiki.common.enums.NodeType;
import com.llmwiki.domain.graph.entity.KgEdge;
import com.llmwiki.domain.graph.entity.KgNode;
import com.llmwiki.domain.graph.repository.KgEdgeRepository;
import com.llmwiki.domain.graph.repository.KgNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeGraphServiceTest {

    @Mock KgNodeRepository nodeRepo;
    @Mock KgEdgeRepository edgeRepo;

    @InjectMocks
    KnowledgeGraphService graphService;

    KgNode entityNode;
    KgNode conceptNode;
    UUID pageId;

    @BeforeEach
    void setUp() {
        pageId = UUID.randomUUID();
        entityNode = KgNode.builder()
                .id(UUID.randomUUID()).name("Java").nodeType(NodeType.ENTITY)
                .description("A programming language").build();
        conceptNode = KgNode.builder()
                .id(UUID.randomUUID()).name("OOP").nodeType(NodeType.CONCEPT)
                .description("Object-Oriented Programming").pageId(pageId).build();
    }

    @Test
    void getAllNodes_shouldReturnAllNodes() {
        when(nodeRepo.findAll()).thenReturn(List.of(entityNode, conceptNode));

        List<KgNode> result = graphService.getAllNodes();

        assertEquals(2, result.size());
    }

    @Test
    void getAllEdges_shouldReturnAllEdges() {
        KgEdge edge = KgEdge.builder()
                .id(UUID.randomUUID()).sourceNodeId(entityNode.getId())
                .targetNodeId(conceptNode.getId()).edgeType(EdgeType.RELATED_TO)
                .weight(BigDecimal.valueOf(0.5)).build();
        when(edgeRepo.findAll()).thenReturn(List.of(edge));

        List<KgEdge> result = graphService.getAllEdges();

        assertEquals(1, result.size());
        assertEquals(EdgeType.RELATED_TO, result.get(0).getEdgeType());
    }

    @Test
    void getGraphData_shouldReturnFormattedGraph() {
        when(nodeRepo.findAll()).thenReturn(List.of(entityNode, conceptNode));
        KgEdge edge = KgEdge.builder()
                .id(UUID.randomUUID()).sourceNodeId(entityNode.getId())
                .targetNodeId(conceptNode.getId()).edgeType(EdgeType.RELATED_TO)
                .weight(BigDecimal.valueOf(0.7)).build();
        when(edgeRepo.findAll()).thenReturn(List.of(edge));

        KnowledgeGraphService.GraphData result = graphService.getGraphData();

        assertEquals(2, result.nodes.size());
        assertEquals(1, result.edges.size());
        assertEquals("Java", result.nodes.get(0).name);
        assertEquals("ENTITY", result.nodes.get(0).type);
        assertEquals(entityNode.getId().toString(), result.edges.get(0).source);
        assertEquals(conceptNode.getId().toString(), result.edges.get(0).target);
        assertEquals(0.7, result.edges.get(0).weight);
    }

    @Test
    void getNeighborhood_shouldReturnNodeAndNeighbors() {
        UUID nodeId = entityNode.getId();
        KgEdge outgoing = KgEdge.builder().id(UUID.randomUUID())
                .sourceNodeId(nodeId).targetNodeId(conceptNode.getId())
                .edgeType(EdgeType.RELATED_TO).weight(BigDecimal.valueOf(0.5)).build();

        when(edgeRepo.findBySourceNodeId(nodeId)).thenReturn(List.of(outgoing));
        when(edgeRepo.findByTargetNodeId(nodeId)).thenReturn(List.of());
        when(nodeRepo.findAllById(any())).thenReturn(List.of(entityNode, conceptNode));

        KnowledgeGraphService.GraphData result = graphService.getNeighborhood(nodeId);

        assertEquals(2, result.nodes.size());
        assertEquals(1, result.edges.size());
    }

    @Test
    void findOrphans_shouldReturnNodesWithNoEdges() {
        KgNode orphan = KgNode.builder()
                .id(UUID.randomUUID()).name("Orphan").nodeType(NodeType.ENTITY)
                .description("No connections").build();

        when(nodeRepo.findOrphanNodes()).thenReturn(List.of(entityNode, orphan));

        List<KgNode> result = graphService.findOrphans();

        assertEquals(2, result.size()); // Both are orphans when no edges exist
        verify(nodeRepo).findOrphanNodes();
        verifyNoMoreInteractions(nodeRepo);
        verifyNoInteractions(edgeRepo);
    }

    @Test
    void findOrphans_shouldExcludeConnectedNodes() {
        KgNode orphan = KgNode.builder()
                .id(UUID.randomUUID()).name("Orphan").nodeType(NodeType.ENTITY)
                .description("No connections").build();

        // Only the orphan is returned by the SQL query — connected nodes are excluded
        when(nodeRepo.findOrphanNodes()).thenReturn(List.of(orphan));

        List<KgNode> result = graphService.findOrphans();

        assertEquals(1, result.size());
        assertEquals("Orphan", result.get(0).getName());
        verify(nodeRepo).findOrphanNodes();
    }

    @Test
    void findOrphans_shouldReturnEmptyWhenNoOrphans() {
        when(nodeRepo.findOrphanNodes()).thenReturn(List.of());

        List<KgNode> result = graphService.findOrphans();

        assertTrue(result.isEmpty());
        verify(nodeRepo).findOrphanNodes();
    }

    @Test
    void findOrphansShouldNotCallFindAllOnNodeOrEdgeRepos() {
        // Given
        when(nodeRepo.findOrphanNodes()).thenReturn(List.of());

        // When
        graphService.findOrphans();

        // Then: verify the old O(n) load-everything methods are NOT called
        verify(nodeRepo, never()).findAll();
        verify(edgeRepo, never()).findAll();
    }

    @Test
    void createNode_shouldSaveAndReturnNode() {
        when(nodeRepo.save(any(KgNode.class))).thenAnswer(i -> {
            KgNode n = i.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        KgNode result = graphService.createNode("Test", NodeType.ENTITY, "Description");

        assertNotNull(result.getId());
        assertEquals("Test", result.getName());
        assertEquals(NodeType.ENTITY, result.getNodeType());
        assertEquals("Description", result.getDescription());
    }

    @Test
    void createEdge_shouldSaveAndReturnEdge() {
        when(edgeRepo.save(any(KgEdge.class))).thenAnswer(i -> {
            KgEdge e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        KgEdge result = graphService.createEdge(
                entityNode.getId(), conceptNode.getId(), EdgeType.PART_OF, 0.8);

        assertNotNull(result.getId());
        assertEquals(entityNode.getId(), result.getSourceNodeId());
        assertEquals(conceptNode.getId(), result.getTargetNodeId());
        assertEquals(EdgeType.PART_OF, result.getEdgeType());
        assertEquals(0.8, result.getWeight().doubleValue());
    }

    @Test
    void deleteNode_shouldRemoveNodeAndItsEdges() {
        UUID nodeId = entityNode.getId();
        KgEdge edge1 = KgEdge.builder().id(UUID.randomUUID())
                .sourceNodeId(nodeId).targetNodeId(conceptNode.getId())
                .edgeType(EdgeType.RELATED_TO).build();
        KgEdge edge2 = KgEdge.builder().id(UUID.randomUUID())
                .sourceNodeId(conceptNode.getId()).targetNodeId(nodeId)
                .edgeType(EdgeType.RELATED_TO).build();

        when(edgeRepo.findBySourceNodeId(nodeId)).thenReturn(List.of(edge1));
        when(edgeRepo.findByTargetNodeId(nodeId)).thenReturn(List.of(edge2));

        graphService.deleteNode(nodeId);

        verify(edgeRepo).delete(edge1);
        verify(edgeRepo).delete(edge2);
        verify(nodeRepo).deleteById(nodeId);
    }

    @Test
    void findShortestPath_shouldReturnDirectPath() {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        KgEdge edge = KgEdge.builder().id(UUID.randomUUID())
                .sourceNodeId(fromId).targetNodeId(toId)
                .edgeType(EdgeType.RELATED_TO).build();

        when(edgeRepo.findBySourceNodeId(fromId)).thenReturn(List.of(edge));
        when(edgeRepo.findByTargetNodeId(fromId)).thenReturn(List.of());

        List<UUID> path = graphService.findShortestPath(fromId, toId);

        assertEquals(2, path.size());
        assertEquals(fromId, path.get(0));
        assertEquals(toId, path.get(1));
    }

    @Test
    void findShortestPath_shouldReturnSameNodeWhenFromEqualsTo() {
        UUID nodeId = UUID.randomUUID();

        List<UUID> path = graphService.findShortestPath(nodeId, nodeId);

        assertEquals(1, path.size());
        assertEquals(nodeId, path.get(0));
        verifyNoInteractions(edgeRepo);
    }

    @Test
    void findShortestPath_shouldReturnEmptyWhenNoPath() {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

        when(edgeRepo.findBySourceNodeId(fromId)).thenReturn(List.of());
        when(edgeRepo.findByTargetNodeId(fromId)).thenReturn(List.of());

        List<UUID> path = graphService.findShortestPath(fromId, toId);

        assertTrue(path.isEmpty());
    }

    @Test
    void findShortestPath_shouldFindMultiHopPath() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();

        KgEdge edgeAB = KgEdge.builder().id(UUID.randomUUID())
                .sourceNodeId(a).targetNodeId(b).edgeType(EdgeType.RELATED_TO).build();
        KgEdge edgeBC = KgEdge.builder().id(UUID.randomUUID())
                .sourceNodeId(b).targetNodeId(c).edgeType(EdgeType.RELATED_TO).build();

        when(edgeRepo.findBySourceNodeId(a)).thenReturn(List.of(edgeAB));
        when(edgeRepo.findByTargetNodeId(a)).thenReturn(List.of());
        when(edgeRepo.findBySourceNodeId(b)).thenReturn(List.of(edgeBC));
        when(edgeRepo.findByTargetNodeId(b)).thenReturn(List.of());

        List<UUID> path = graphService.findShortestPath(a, c);

        assertEquals(3, path.size());
        assertEquals(a, path.get(0));
        assertEquals(b, path.get(1));
        assertEquals(c, path.get(2));
    }

    @Test
    void findShortestPath_shouldTraverseIncomingEdges() {
        UUID fromId = UUID.randomUUID();
        UUID midId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

        KgEdge edgeToMid = KgEdge.builder().id(UUID.randomUUID())
                .sourceNodeId(midId).targetNodeId(fromId).edgeType(EdgeType.RELATED_TO).build();
        KgEdge edgeMidTo = KgEdge.builder().id(UUID.randomUUID())
                .sourceNodeId(midId).targetNodeId(toId).edgeType(EdgeType.RELATED_TO).build();

        when(edgeRepo.findBySourceNodeId(fromId)).thenReturn(List.of());
        when(edgeRepo.findByTargetNodeId(fromId)).thenReturn(List.of(edgeToMid));
        when(edgeRepo.findBySourceNodeId(midId)).thenReturn(List.of(edgeMidTo));
        when(edgeRepo.findByTargetNodeId(midId)).thenReturn(List.of());

        List<UUID> path = graphService.findShortestPath(fromId, toId);

        assertEquals(3, path.size());
        assertEquals(fromId, path.get(0));
        assertEquals(midId, path.get(1));
        assertEquals(toId, path.get(2));
    }

    @Test
    void getGraphStats_shouldReturnCounts() {
        when(nodeRepo.count()).thenReturn(10L);
        when(edgeRepo.count()).thenReturn(20L);
        when(nodeRepo.findOrphanNodes()).thenReturn(List.of());
        for (NodeType type : NodeType.values()) {
            when(nodeRepo.countByNodeType(type)).thenReturn(3L);
        }
        for (EdgeType type : EdgeType.values()) {
            when(edgeRepo.countByEdgeType(type)).thenReturn(2L);
        }

        Map<String, Object> stats = graphService.getGraphStats();

        assertEquals(10L, stats.get("totalNodes"));
        assertEquals(20L, stats.get("totalEdges"));
        assertEquals(0, stats.get("orphanCount"));
        assertNotNull(stats.get("nodeTypeCounts"));
        assertNotNull(stats.get("edgeTypeCounts"));
    }

    @Test
    void getGraphStats_shouldIncludeOrphanCount() {
        KgNode orphan = KgNode.builder()
                .id(UUID.randomUUID()).name("Orphan").nodeType(NodeType.ENTITY).build();
        when(nodeRepo.count()).thenReturn(5L);
        when(edgeRepo.count()).thenReturn(2L);
        when(nodeRepo.findOrphanNodes()).thenReturn(List.of(orphan));
        for (NodeType type : NodeType.values()) {
            when(nodeRepo.countByNodeType(type)).thenReturn(1L);
        }
        for (EdgeType type : EdgeType.values()) {
            when(edgeRepo.countByEdgeType(type)).thenReturn(1L);
        }

        Map<String, Object> stats = graphService.getGraphStats();

        assertEquals(1, stats.get("orphanCount"));
    }

    @Test
    void getGraphStats_shouldWorkWithEmptyGraph() {
        when(nodeRepo.count()).thenReturn(0L);
        when(edgeRepo.count()).thenReturn(0L);
        when(nodeRepo.findOrphanNodes()).thenReturn(List.of());
        for (NodeType type : NodeType.values()) {
            when(nodeRepo.countByNodeType(type)).thenReturn(0L);
        }
        for (EdgeType type : EdgeType.values()) {
            when(edgeRepo.countByEdgeType(type)).thenReturn(0L);
        }

        Map<String, Object> stats = graphService.getGraphStats();

        assertEquals(0L, stats.get("totalNodes"));
        assertEquals(0L, stats.get("totalEdges"));
    }
}
