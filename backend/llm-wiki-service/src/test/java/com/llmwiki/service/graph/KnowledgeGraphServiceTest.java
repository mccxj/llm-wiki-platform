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

        when(nodeRepo.findAll()).thenReturn(List.of(entityNode, orphan));
        when(edgeRepo.findAll()).thenReturn(List.of());

        List<KgNode> result = graphService.findOrphans();

        assertEquals(2, result.size()); // Both are orphans when no edges exist
    }

    @Test
    void findOrphans_shouldExcludeConnectedNodes() {
        when(nodeRepo.findAll()).thenReturn(List.of(entityNode, conceptNode));
        KgEdge edge = KgEdge.builder()
                .sourceNodeId(entityNode.getId()).targetNodeId(conceptNode.getId())
                .edgeType(EdgeType.RELATED_TO).build();
        when(edgeRepo.findAll()).thenReturn(List.of(edge));

        List<KgNode> result = graphService.findOrphans();

        assertEquals(0, result.size()); // Both connected, no orphans
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
}
