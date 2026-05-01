package com.llmwiki.web.controller;

import com.llmwiki.common.enums.EdgeType;
import com.llmwiki.common.enums.NodeType;
import com.llmwiki.domain.graph.entity.KgEdge;
import com.llmwiki.domain.graph.entity.KgNode;
import com.llmwiki.service.graph.KnowledgeGraphService;
import com.llmwiki.service.graph.KnowledgeGraphService.GraphData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GraphControllerTest {

    @Mock KnowledgeGraphService graphService;
    @InjectMocks GraphController controller;

    @Test
    void getGraph_shouldReturnGraphData() {
        GraphData expected = new GraphData();
        expected.nodes = List.of();
        expected.edges = List.of();
        when(graphService.getGraphData()).thenReturn(expected);

        var response = controller.getGraph();

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
    }

    @Test
    void getNeighborhood_shouldReturnSubgraph() {
        UUID nodeId = UUID.randomUUID();
        GraphData expected = new GraphData();
        expected.nodes = List.of();
        expected.edges = List.of();
        when(graphService.getNeighborhood(nodeId)).thenReturn(expected);

        var response = controller.getNeighborhood(nodeId);

        assertEquals(200, response.getStatusCodeValue());
        verify(graphService).getNeighborhood(nodeId);
    }

    @Test
    void getNodes_shouldReturnAllNodes() {
        List<KgNode> expected = List.of(KgNode.builder().name("Java").build());
        when(graphService.getAllNodes()).thenReturn(expected);

        var response = controller.getNodes();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getEdges_shouldReturnAllEdges() {
        List<KgEdge> expected = List.of(KgEdge.builder().build());
        when(graphService.getAllEdges()).thenReturn(expected);

        var response = controller.getEdges();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getOrphans_shouldReturnOrphanNodes() {
        List<KgNode> expected = List.of(KgNode.builder().name("Orphan").build());
        when(graphService.findOrphans()).thenReturn(expected);

        var response = controller.getOrphans();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void createNode_shouldDelegateToService() {
        KgNode expected = KgNode.builder().id(UUID.randomUUID())
                .name("Test").nodeType(NodeType.ENTITY).build();
        when(graphService.createNode("Test", NodeType.ENTITY, "desc")).thenReturn(expected);

        var response = controller.createNode(Map.of("name", "Test", "type", "ENTITY", "description", "desc"));

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Test", response.getBody().getName());
    }

    @Test
    void createEdge_shouldDelegateToService() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        KgEdge expected = KgEdge.builder().id(UUID.randomUUID())
                .sourceNodeId(sourceId).targetNodeId(targetId)
                .edgeType(EdgeType.RELATED_TO).weight(BigDecimal.valueOf(0.5)).build();
        when(graphService.createEdge(sourceId, targetId, EdgeType.RELATED_TO, 0.5)).thenReturn(expected);

        var response = controller.createEdge(Map.of(
                "sourceId", sourceId.toString(), "targetId", targetId.toString(),
                "type", "RELATED_TO", "weight", "0.5"));

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(sourceId, response.getBody().getSourceNodeId());
    }

    @Test
    void deleteNode_shouldReturnOk() {
        UUID nodeId = UUID.randomUUID();
        doNothing().when(graphService).deleteNode(nodeId);

        var response = controller.deleteNode(nodeId);

        assertEquals(200, response.getStatusCodeValue());
        verify(graphService).deleteNode(nodeId);
    }
}
