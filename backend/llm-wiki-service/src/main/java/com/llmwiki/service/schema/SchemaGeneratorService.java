package com.llmwiki.service.schema;

import com.llmwiki.common.enums.EdgeType;
import com.llmwiki.common.enums.NodeType;
import com.llmwiki.domain.graph.repository.KgEdgeRepository;
import com.llmwiki.domain.graph.repository.KgNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * SCHEMA.md 自动生成服务
 * Karpathy 第三层：自动汇总知识图谱结构
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchemaGeneratorService {

    private final KgNodeRepository kgNodeRepo;
    private final KgEdgeRepository kgEdgeRepo;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    /**
     * 生成 SCHEMA.md 文件
     */
    public void generateSchema() {
        StringBuilder sb = new StringBuilder();
        sb.append("# SCHEMA.md — Knowledge Graph Schema\n\n");
        sb.append("**Generated:** ").append(FMT.format(Instant.now())).append("\n\n");

        // Overview
        long nodeCount = kgNodeRepo.count();
        long edgeCount = kgEdgeRepo.count();
        sb.append("## Overview\n\n");
        sb.append("- **Total Nodes:** ").append(nodeCount).append("\n");
        sb.append("- **Total Edges:** ").append(edgeCount).append("\n\n");

        // Node Types
        sb.append("## Node Types\n\n");
        sb.append("| Type | Count |\n");
        sb.append("|------|-------|\n");
        for (NodeType type : NodeType.values()) {
            long count = kgNodeRepo.countByNodeType(type);
            sb.append("| ").append(type.name()).append(" | ").append(count).append(" |\n");
        }
        sb.append("\n");

        // Edge Types
        sb.append("## Edge Types\n\n");
        sb.append("| Type | Count |\n");
        sb.append("|------|-------|\n");
        for (EdgeType type : EdgeType.values()) {
            long count = kgEdgeRepo.countByEdgeType(type);
            sb.append("| ").append(type.name()).append(" | ").append(count).append(" |\n");
        }
        sb.append("\n");

        // Entity Sub-types
        sb.append("## Top Entities\n\n");
        sb.append("See the knowledge graph for the most connected entities.\n\n");

        // Write to file
        Path outputPath = Path.of("backend/llm-wiki-web/src/main/resources/SCHEMA.md");
        try {
            Files.writeString(outputPath, sb.toString());
            log.info("SCHEMA.md generated successfully at {}", outputPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to write SCHEMA.md", e);
        }
    }
}
