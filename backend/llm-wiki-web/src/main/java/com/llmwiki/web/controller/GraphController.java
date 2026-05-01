package com.llmwiki.web.controller;

import com.llmwiki.common.enums.EdgeType;
import com.llmwiki.common.enums.NodeType;
import com.llmwiki.domain.graph.entity.KgEdge;
import com.llmwiki.domain.graph.entity.KgNode;
import com.llmwiki.service.graph.KnowledgeGraphService;
import com.llmwiki.service.graph.KnowledgeGraphService.GraphData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/graph")
@RequiredArgsConstructor
public class GraphController {

    private final KnowledgeGraphService graphService;

    @GetMapping
    public ResponseEntity<GraphData> getGraph() {
        return ResponseEntity.ok(graphService.getGraphData());
    }

    @GetMapping("/neighborhood/{nodeId}")
    public ResponseEntity<GraphData> getNeighborhood(@PathVariable UUID nodeId) {
        return ResponseEntity.ok(graphService.getNeighborhood(nodeId));
    }

    @GetMapping("/nodes")
    public ResponseEntity<List<KgNode>> getNodes() {
        return ResponseEntity.ok(graphService.getAllNodes());
    }

    @GetMapping("/edges")
    public ResponseEntity<List<KgEdge>> getEdges() {
        return ResponseEntity.ok(graphService.getAllEdges());
    }

    @GetMapping("/orphans")
    public ResponseEntity<List<KgNode>> getOrphans() {
        return ResponseEntity.ok(graphService.findOrphans());
    }

    @PostMapping("/nodes")
    public ResponseEntity<KgNode> createNode(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        NodeType type = NodeType.valueOf(body.getOrDefault("type", "ENTITY"));
        String description = body.getOrDefault("description", "");
        return ResponseEntity.ok(graphService.createNode(name, type, description));
    }

    @PostMapping("/edges")
    public ResponseEntity<KgEdge> createEdge(@RequestBody Map<String, String> body) {
        UUID sourceId = UUID.fromString(body.get("sourceId"));
        UUID targetId = UUID.fromString(body.get("targetId"));
        EdgeType type = EdgeType.valueOf(body.getOrDefault("type", "RELATED_TO"));
        double weight = Double.parseDouble(body.getOrDefault("weight", "0.5"));
        return ResponseEntity.ok(graphService.createEdge(sourceId, targetId, type, weight));
    }

    @DeleteMapping("/nodes/{nodeId}")
    public ResponseEntity<Void> deleteNode(@PathVariable UUID nodeId) {
        graphService.deleteNode(nodeId);
        return ResponseEntity.ok().build();
    }
}
