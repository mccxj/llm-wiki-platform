package com.llmwiki.service.graph;

import com.llmwiki.adapter.api.EmbeddingClient;
import com.llmwiki.common.enums.NodeType;
import com.llmwiki.domain.graph.entity.KgNode;
import com.llmwiki.domain.graph.entity.KgVector;
import com.llmwiki.domain.graph.repository.KgNodeRepository;
import com.llmwiki.domain.graph.repository.KgVectorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * E-7: Semantic deduplication using embedding similarity.
 * Replaces name-based exact matching with vector similarity comparison.
 */
@Service
public class SemanticDedupService {

    private static final Logger log = LoggerFactory.getLogger(SemanticDedupService.class);

    private final EmbeddingClient embeddingClient;
    private final KgNodeRepository kgNodeRepo;
    private final KgVectorRepository kgVectorRepo;

    private final double similarityThreshold;
    private final double warnThreshold;

    public SemanticDedupService(
            EmbeddingClient embeddingClient,
            KgNodeRepository kgNodeRepo,
            KgVectorRepository kgVectorRepo,
            @Value("${llm.wiki.dedup.similarity-threshold:0.85}") double similarityThreshold,
            @Value("${llm.wiki.dedup.warn-threshold:0.70}") double warnThreshold) {
        this.embeddingClient = embeddingClient;
        this.kgNodeRepo = kgNodeRepo;
        this.kgVectorRepo = kgVectorRepo;
        this.similarityThreshold = similarityThreshold;
        this.warnThreshold = warnThreshold;
    }

    /**
     * Check if a new entity is a duplicate of an existing KG node.
     * Returns the existing node if similarity >= threshold, or null if new.
     */
    public DedupResult checkDuplicate(String entityName, String entityType, String description) {
        try {
            String text = entityName + ": " + (description != null ? description : "");
            float[] embedding = embeddingClient.embed(text);
            String vectorStr = embeddingToJson(embedding);

            // Get all existing vectors (similarity filtering in service layer for H2 compatibility)
            List<KgVector> candidates = kgVectorRepo.findByNodeIdNot(UUID.randomUUID());

            KgNode bestMatch = null;
            double bestSimilarity = 0;

            for (KgVector candidate : candidates) {
                KgNode node = kgNodeRepo.findById(candidate.getNodeId()).orElse(null);
                if (node == null || node.getNodeType() != NodeType.ENTITY) continue;

                double similarity = cosineSimilarity(embedding, candidate.getVector());
                if (similarity > bestSimilarity) {
                    bestSimilarity = similarity;
                    bestMatch = node;
                }
            }

            if (bestSimilarity >= similarityThreshold) {
                log.info("Semantic dedup: '{}' matches '{}' (similarity={})",
                        entityName, bestMatch.getName(), String.format("%.2f", bestSimilarity));
                return new DedupResult(DedupAction.MERGE, bestMatch, bestSimilarity);
            } else if (bestSimilarity >= warnThreshold) {
                log.info("Semantic dedup: '{}' similar to '{}' (similarity={}) — candidate",
                        entityName, bestMatch.getName(), String.format("%.2f", bestSimilarity));
                return new DedupResult(DedupAction.CANDIDATE, bestMatch, bestSimilarity);
            }

            return new DedupResult(DedupAction.NEW, null, bestSimilarity);
        } catch (Exception e) {
            log.warn("Semantic dedup failed for '{}': {}", entityName, e.getMessage());
            return new DedupResult(DedupAction.NEW, null, 0);
        }
    }

    /** Cosine similarity between two vectors. */
    public static double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private String embeddingToJson(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    public enum DedupAction { MERGE, CANDIDATE, NEW }

    public record DedupResult(DedupAction action, KgNode existingNode, double similarity) {}
}
