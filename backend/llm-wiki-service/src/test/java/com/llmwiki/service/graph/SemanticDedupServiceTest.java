package com.llmwiki.service.graph;

import com.llmwiki.adapter.api.EmbeddingClient;
import com.llmwiki.common.enums.NodeType;
import com.llmwiki.domain.graph.entity.KgNode;
import com.llmwiki.domain.graph.entity.KgVector;
import com.llmwiki.domain.graph.repository.KgNodeRepository;
import com.llmwiki.domain.graph.repository.KgVectorRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SemanticDedupServiceTest {

    @Mock
    EmbeddingClient embeddingClient;
    @Mock
    EntityManager entityManager;
    @Mock
    Query idQuery;
    @Mock
    KgNodeRepository kgNodeRepo;
    @Mock
    KgVectorRepository kgVectorRepo;

    SemanticDedupService service;

    @BeforeEach
    void setUp() {
        service = new SemanticDedupService(embeddingClient, entityManager, kgNodeRepo, kgVectorRepo, 0.85, 0.70);
    }

    @Test
    void cosineSimilarity_identicalVectors_returnsOne() {
        float[] a = {1, 0, 0};
        assertEquals(1.0, SemanticDedupService.cosineSimilarity(a, a), 0.001);
    }

    @Test
    void cosineSimilarity_orthogonalVectors_returnsZero() {
        float[] a = {1, 0, 0};
        float[] b = {0, 1, 0};
        assertEquals(0.0, SemanticDedupService.cosineSimilarity(a, b), 0.001);
    }

    @Test
    void cosineSimilarity_oppositeVectors_returnsNegativeOne() {
        float[] a = {1, 0, 0};
        float[] b = {-1, 0, 0};
        assertEquals(-1.0, SemanticDedupService.cosineSimilarity(a, b), 0.001);
    }

    @Test
    void cosineSimilarity_differentLength_returnsZero() {
        float[] a = {1, 0};
        float[] b = {1, 0, 0};
        assertEquals(0.0, SemanticDedupService.cosineSimilarity(a, b), 0.001);
    }

    @Test
    void checkDuplicate_highSimilarity_returnsMerge() {
        float[] emb = {1, 0, 0};
        when(embeddingClient.embed(anyString())).thenReturn(emb);

        UUID nodeId = UUID.randomUUID();
        KgNode existingNode = KgNode.builder()
                .id(nodeId).name("Java").nodeType(NodeType.ENTITY).build();
        float[] existingVector = {1, 0, 0};

        // Mock: DB returns candidate IDs (single-column query)
        when(entityManager.createNativeQuery(anyString())).thenReturn(idQuery);
        when(idQuery.setParameter(eq("queryVector"), anyString())).thenReturn(idQuery);
        when(idQuery.setParameter(eq("excludeId"), any(UUID.class))).thenReturn(idQuery);
        when(idQuery.setParameter(eq("maxDistance"), any(Double.class))).thenReturn(idQuery);
        when(idQuery.getResultList()).thenReturn(List.of(nodeId));

        // Mock: fetch node and vector for similarity computation
        when(kgNodeRepo.findById(nodeId)).thenReturn(Optional.of(existingNode));
        when(kgVectorRepo.findByNodeId(nodeId)).thenReturn(Optional.of(
                KgVector.builder().nodeId(nodeId).vector(existingVector).build()));

        var result = service.checkDuplicate("Java", "TECH", "Programming language");

        assertEquals(SemanticDedupService.DedupAction.MERGE, result.action());
        assertNotNull(result.existingNode());
        assertEquals("Java", result.existingNode().getName());
        assertTrue(result.similarity() >= 0.85);
    }

    @Test
    void checkDuplicate_mediumSimilarity_returnsCandidate() {
        // Similar but not identical vector → cosine ≈ 0.71 (between 0.70 and 0.85)
        float[] emb1 = {1, 0, 0};
        float[] emb2 = {0.7f, 0.714f, 0};
        when(embeddingClient.embed(anyString())).thenReturn(emb1);

        UUID nodeId = UUID.randomUUID();
        KgNode existingNode = KgNode.builder()
                .id(nodeId).name("Java Platform").nodeType(NodeType.ENTITY).build();

        when(entityManager.createNativeQuery(anyString())).thenReturn(idQuery);
        when(idQuery.setParameter(eq("queryVector"), anyString())).thenReturn(idQuery);
        when(idQuery.setParameter(eq("excludeId"), any(UUID.class))).thenReturn(idQuery);
        when(idQuery.setParameter(eq("maxDistance"), any(Double.class))).thenReturn(idQuery);
        when(idQuery.getResultList()).thenReturn(List.of(nodeId));

        when(kgNodeRepo.findById(nodeId)).thenReturn(Optional.of(existingNode));
        when(kgVectorRepo.findByNodeId(nodeId)).thenReturn(Optional.of(
                KgVector.builder().nodeId(nodeId).vector(emb2).build()));

        var result = service.checkDuplicate("Java", "TECH", "Programming language");

        assertEquals(SemanticDedupService.DedupAction.CANDIDATE, result.action());
        assertTrue(result.similarity() >= 0.70);
        assertTrue(result.similarity() < 0.85);
    }

    @Test
    void checkDuplicate_lowSimilarity_returnsNew() {
        // Orthogonal vectors → cosine similarity = 0 (far below warnThreshold 0.70)
        float[] emb1 = {1, 0, 0};
        float[] emb2 = {0, 1, 0};
        when(embeddingClient.embed(anyString())).thenReturn(emb1);

        UUID nodeId = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(idQuery);
        when(idQuery.setParameter(eq("queryVector"), anyString())).thenReturn(idQuery);
        when(idQuery.setParameter(eq("excludeId"), any(UUID.class))).thenReturn(idQuery);
        when(idQuery.setParameter(eq("maxDistance"), any(Double.class))).thenReturn(idQuery);
        when(idQuery.getResultList()).thenReturn(List.of(nodeId));

        when(kgNodeRepo.findById(nodeId)).thenReturn(Optional.empty()); // node deleted

        var result = service.checkDuplicate("Java", "TECH", "Programming language");

        assertEquals(SemanticDedupService.DedupAction.NEW, result.action());
    }

    @Test
    void checkDuplicate_embeddingFailure_returnsNew() {
        when(embeddingClient.embed(anyString())).thenThrow(new RuntimeException("API error"));

        var result = service.checkDuplicate("Java", "TECH", "Programming language");

        assertEquals(SemanticDedupService.DedupAction.NEW, result.action());
        assertEquals(0, result.similarity());
    }

    @Test
    void checkDuplicate_noCandidates_returnsNew() {
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{1, 0, 0});
        when(entityManager.createNativeQuery(anyString())).thenReturn(idQuery);
        when(idQuery.setParameter(eq("queryVector"), anyString())).thenReturn(idQuery);
        when(idQuery.setParameter(eq("excludeId"), any(UUID.class))).thenReturn(idQuery);
        when(idQuery.setParameter(eq("maxDistance"), any(Double.class))).thenReturn(idQuery);
        when(idQuery.getResultList()).thenReturn(List.of());

        var result = service.checkDuplicate("Java", "TECH", "Programming language");

        assertEquals(SemanticDedupService.DedupAction.NEW, result.action());
    }
}
