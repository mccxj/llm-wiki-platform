package com.llmwiki.service.graph;

import com.llmwiki.common.enums.EdgeType;
import com.llmwiki.common.enums.NodeType;
import com.llmwiki.domain.graph.entity.KgEdge;
import com.llmwiki.domain.graph.entity.KgNode;
import com.llmwiki.domain.graph.repository.KgEdgeRepository;
import com.llmwiki.domain.graph.repository.KgNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeGraphService {

    private final KgNodeRepository nodeRepo;
    private final KgEdgeRepository edgeRepo;

    /**
     * Get all nodes (for graph visualization).
     */
    public List<KgNode> getAllNodes() {
        return nodeRepo.findAll();
    }

    /**
     * Get all edges (for graph visualization).
     */
    public List<KgEdge> getAllEdges() {
        return edgeRepo.findAll();
    }

    /**
     * Get graph data formatted for visualization.
     */
    public GraphData getGraphData() {
        List<KgNode> nodes = nodeRepo.findAll();
        List<KgEdge> edges = edgeRepo.findAll();

        GraphData data = new GraphData();
        data.nodes = nodes.stream().map(n -> {
            GraphNode gn = new GraphNode();
            gn.id = n.getId().toString();
            gn.name = n.getName();
            gn.type = n.getNodeType().name();
            gn.description = n.getDescription();
            return gn;
        }).toList();

        data.edges = edges.stream().map(e -> {
            GraphEdge ge = new GraphEdge();
            ge.source = e.getSourceNodeId().toString();
            ge.target = e.getTargetNodeId().toString();
            ge.type = e.getEdgeType().name();
            ge.weight = e.getWeight().doubleValue();
            return ge;
        }).toList();

        return data;
    }

    /**
     * Get a node with its neighbors (for expand-in-visualization).
     */
    public GraphData getNeighborhood(UUID nodeId) {
        Set<UUID> neighborIds = new HashSet<>();
        neighborIds.add(nodeId);

        List<KgEdge> outgoing = edgeRepo.findBySourceNodeId(nodeId);
        List<KgEdge> incoming = edgeRepo.findByTargetNodeId(nodeId);

        outgoing.forEach(e -> { neighborIds.add(e.getSourceNodeId()); neighborIds.add(e.getTargetNodeId()); });
        incoming.forEach(e -> { neighborIds.add(e.getSourceNodeId()); neighborIds.add(e.getTargetNodeId()); });

        List<KgNode> nodes = nodeRepo.findAllById(neighborIds);
        List<KgEdge> edges = new ArrayList<>();
        edges.addAll(outgoing);
        edges.addAll(incoming);

        GraphData data = new GraphData();
        data.nodes = nodes.stream().map(n -> {
            GraphNode gn = new GraphNode();
            gn.id = n.getId().toString();
            gn.name = n.getName();
            gn.type = n.getNodeType().name();
            gn.description = n.getDescription();
            return gn;
        }).toList();

        data.edges = edges.stream().map(e -> {
            GraphEdge ge = new GraphEdge();
            ge.source = e.getSourceNodeId().toString();
            ge.target = e.getTargetNodeId().toString();
            ge.type = e.getEdgeType().name();
            ge.weight = e.getWeight().doubleValue();
            return ge;
        }).toList();

        return data;
    }

    /**
     * Find orphan nodes (no edges).
     */
    public List<KgNode> findOrphans() {
        List<KgNode> allNodes = nodeRepo.findAll();
        List<KgEdge> allEdges = edgeRepo.findAll();
        Set<UUID> connectedIds = new HashSet<>();
        allEdges.forEach(e -> { connectedIds.add(e.getSourceNodeId()); connectedIds.add(e.getTargetNodeId()); });

        return allNodes.stream()
                .filter(n -> !connectedIds.contains(n.getId()))
                .toList();
    }

    /**
     * Create a new node.
     */
    @Transactional
    public KgNode createNode(String name, NodeType type, String description) {
        KgNode node = KgNode.builder()
                .name(name)
                .nodeType(type)
                .description(description)
                .build();
        return nodeRepo.save(node);
    }

    /**
     * Create a new edge.
     */
    @Transactional
    public KgEdge createEdge(UUID sourceId, UUID targetId, EdgeType type, double weight) {
        KgEdge edge = KgEdge.builder()
                .sourceNodeId(sourceId)
                .targetNodeId(targetId)
                .edgeType(type)
                .weight(java.math.BigDecimal.valueOf(weight))
                .build();
        return edgeRepo.save(edge);
    }

    /**
     * Delete a node and its edges.
     */
    @Transactional
    public void deleteNode(UUID nodeId) {
        edgeRepo.findBySourceNodeId(nodeId).forEach(e -> edgeRepo.delete(e));
        edgeRepo.findByTargetNodeId(nodeId).forEach(e -> edgeRepo.delete(e));
        nodeRepo.deleteById(nodeId);
    }

    /**
     * DTOs for graph visualization.
     */
    public static class GraphData {
        public List<GraphNode> nodes;
        public List<GraphEdge> edges;
    }

    public static class GraphNode {
        public String id;
        public String name;
        public String type;
        public String description;
    }

    public static class GraphEdge {
        public String source;
        public String target;
        public String type;
        public double weight;
    }
}
