package com.llmwiki.service.graph;

import com.llmwiki.adapter.api.EmbeddingClient;
import com.llmwiki.common.enums.NodeType;
import com.llmwiki.domain.graph.entity.KgNode;
import com.llmwiki.domain.graph.entity.KgVector;
import com.llmwiki.domain.graph.repository.KgNodeRepository;
import com.llmwiki.domain.graph.repository.KgVectorRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
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
    private final EntityManager entityManager;
    private final KgNodeRepository kgNodeRepo;
    private final KgVectorRepository kgVectorRepo;

    private final double similarityThreshold;
    private final double warnThreshold;

    public SemanticDedupService(
            EmbeddingClient embeddingClient,
            EntityManager entityManager,
            KgNodeRepository kgNodeRepo,
            KgVectorRepository kgVectorRepo,
            @Value("${llm.wiki.dedup.similarity-threshold:0.85}") double similarityThreshold,
            @Value("${llm.wiki.dedup.warn-threshold:0.70}") double warnThreshold) {
        this.embeddingClient = embeddingClient;
        this.entityManager = entityManager;
        this.kgNodeRepo = kgNodeRepo;
        this.kgVectorRepo = kgVectorRepo;
        this.similarityThreshold = similarityThreshold;
        this.warnThreshold = warnThreshold;
    }

    /**
     * Check if a new entity is a duplicate of an existing KG node.
     * Returns the existing node if similarity >= threshold, or null if new.
     * Uses MariaDB VEC_DISTANCE() as a SQL pre-filter — joins kg_nodes to restrict
     * the scan to ENTITY nodes at the database level, then applies a distance cap so
     * the DB never materialises vectors that are too far away to be relevant.
     */
    public DedupResult checkDuplicate(String entityName, String entityType, String description) {
        try {
            String text = entityName + ": " + (description != null ? description : "");
            float[] embedding = embeddingClient.embed(text);
            String vectorStr = embeddingToJson(embedding);

            // Compute max distance corresponding to the warnThreshold so MariaDB can
            // skip distant rows early (pre-filter at storage engine level).
            // similarity = 1 / (1 + distance)  →  distance = (1 - s) / s
            double maxDistance = (1.0 - warnThreshold) / warnThreshold;

            // Pre-filter: get candidate node IDs within the distance threshold using VEC_DISTANCE.
            // Returns List<Object> (single-column scalar results).
            Query idQuery = entityManager.createNativeQuery(
                "SELECT v.node_id " +
                "FROM kg_vectors v " +
                "JOIN kg_nodes n ON v.node_id = n.id " +
                "WHERE n.node_type = 'ENTITY' " +
                "  AND v.node_id != :excludeId " +
                "  AND VEC_DISTANCE(v.vector, VEC_FromText(:queryVector)) <= :maxDistance " +
                "ORDER BY VEC_DISTANCE(v.vector, VEC_FromText(:queryVector)) ASC LIMIT 20");
            idQuery.setParameter("queryVector", vectorStr);
            idQuery.setParameter("excludeId", UUID.randomUUID()); // Exclude self
            idQuery.setParameter("maxDistance", maxDistance);
            @SuppressWarnings("unchecked")
            List<Object> candidateIds = idQuery.getResultList();

            if (candidateIds.isEmpty()) {
                return new DedupResult(DedupAction.NEW, null, 0);
            }

            // ENTITY filter is applied in SQL; load candidates and compute exact similarity.
            KgNode bestMatch = null;
            double bestSimilarity = 0;

            for (Object idObj : candidateIds) {
                UUID nodeId;
                try {
                    nodeId = (idObj instanceof UUID) ? (UUID) idObj : UUID.fromString(idObj.toString());
                } catch (Exception e) {
                    continue;
                }

                KgNode node = kgNodeRepo.findById(nodeId).orElse(null);
                if (node == null) continue;

                // Fetch vector for cosine similarity
                float[] candidateVector = kgVectorRepo.findByNodeId(nodeId)
                        .map(KgVector::getVector)
                        .orElse(null);
                if (candidateVector == null) continue;

                double similarity = cosineSimilarity(embedding, candidateVector);
                if (similarity > bestSimilarity) {
                    bestSimilarity = similarity;
                    bestMatch = node;
                }
            }

            if (bestMatch == null) {
                return new DedupResult(DedupAction.NEW, null, 0);
            }

            double similarity = bestSimilarity;

            if (similarity >= similarityThreshold) {
                log.info("Semantic dedup: '{}' matches '{}' (similarity={})",
                        entityName, bestMatch.getName(), String.format("%.2f", similarity));
                return new DedupResult(DedupAction.MERGE, bestMatch, similarity);
            } else if (similarity >= warnThreshold) {
                log.info("Semantic dedup: '{}' similar to '{}' (similarity={}) — candidate",
                        entityName, bestMatch.getName(), String.format("%.2f", similarity));
                return new DedupResult(DedupAction.CANDIDATE, bestMatch, similarity);
            }

            return new DedupResult(DedupAction.NEW, null, similarity);
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
