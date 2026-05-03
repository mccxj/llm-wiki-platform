package com.llmwiki.service.graph;

import com.llmwiki.adapter.api.EmbeddingClient;
import com.llmwiki.common.enums.NodeType;
import com.llmwiki.domain.graph.entity.KgNode;
import com.llmwiki.domain.graph.entity.KgVector;
import com.llmwiki.domain.graph.repository.KgNodeRepository;
import com.llmwiki.domain.graph.repository.KgVectorRepository;
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

    @Mock EmbeddingClient embeddingClient;
    @Mock KgNodeRepository kgNodeRepo;
    @Mock KgVectorRepository kgVectorRepo;

    SemanticDedupService service;

    @BeforeEach
    void setUp() {
        service = new SemanticDedupService(embeddingClient, kgNodeRepo, kgVectorRepo, 0.85, 0.70);
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

        KgNode existingNode = KgNode.builder()
                .id(UUID.randomUUID()).name("Java").nodeType(NodeType.ENTITY).build();
        KgVector existingVector = KgVector.builder()
                .nodeId(existingNode.getId()).vector(emb).build();

        when(kgVectorRepo.findByNodeIdNot(any(UUID.class)))
                .thenReturn(List.of(existingVector));
        when(kgNodeRepo.findById(existingNode.getId()))
                .thenReturn(Optional.of(existingNode));

        var result = service.checkDuplicate("Java", "TECH", "Programming language");

        assertEquals(SemanticDedupService.DedupAction.MERGE, result.action());
        assertNotNull(result.existingNode());
        assertEquals("Java", result.existingNode().getName());
        assertTrue(result.similarity() >= 0.85);
    }

    @Test
    void checkDuplicate_mediumSimilarity_returnsCandidate() {
        float[] emb1 = {1, 0, 0};
        float[] emb2 = {0.8f, 0.6f, 0}; // cosine similarity = 0.8
        when(embeddingClient.embed(anyString())).thenReturn(emb1);

        KgNode existingNode = KgNode.builder()
                .id(UUID.randomUUID()).name("Java Platform").nodeType(NodeType.ENTITY).build();
        KgVector existingVector = KgVector.builder()
                .nodeId(existingNode.getId()).vector(emb2).build();

        when(kgVectorRepo.findByNodeIdNot(any(UUID.class)))
                .thenReturn(List.of(existingVector));
        when(kgNodeRepo.findById(existingNode.getId()))
                .thenReturn(Optional.of(existingNode));

        var result = service.checkDuplicate("Java", "TECH", "Programming language");

        assertEquals(SemanticDedupService.DedupAction.CANDIDATE, result.action());
        assertTrue(result.similarity() >= 0.70);
        assertTrue(result.similarity() < 0.85);
    }

    @Test
    void checkDuplicate_lowSimilarity_returnsNew() {
        float[] emb1 = {1, 0, 0};
        float[] emb2 = {0, 1, 0}; // orthogonal, similarity = 0
        when(embeddingClient.embed(anyString())).thenReturn(emb1);

        KgNode existingNode = KgNode.builder()
                .id(UUID.randomUUID()).name("Python").nodeType(NodeType.ENTITY).build();
        KgVector existingVector = KgVector.builder()
                .nodeId(existingNode.getId()).vector(emb2).build();

        when(kgVectorRepo.findByNodeIdNot(any(UUID.class)))
                .thenReturn(List.of(existingVector));
        when(kgNodeRepo.findById(existingNode.getId()))
                .thenReturn(Optional.of(existingNode));

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
        when(kgVectorRepo.findByNodeIdNot(any(UUID.class)))
                .thenReturn(List.of());

        var result = service.checkDuplicate("Java", "TECH", "Programming language");

        assertEquals(SemanticDedupService.DedupAction.NEW, result.action());
    }
}
