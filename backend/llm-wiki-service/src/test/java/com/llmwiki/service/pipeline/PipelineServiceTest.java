package com.llmwiki.service.pipeline;

import com.llmwiki.adapter.api.AiApiClient;
import com.llmwiki.adapter.api.EmbeddingClient;
import com.llmwiki.adapter.dto.ExtractionResult;
import com.llmwiki.adapter.dto.ExtractionResult.ConceptInfo;
import com.llmwiki.adapter.dto.ExtractionResult.EntityInfo;
import com.llmwiki.adapter.dto.ScoreResult;
import com.llmwiki.common.enums.*;
import com.llmwiki.domain.approval.repository.ApprovalQueueRepository;
import com.llmwiki.domain.config.repository.SystemConfigRepository;
import com.llmwiki.domain.graph.entity.KgEdge;
import com.llmwiki.domain.graph.entity.KgNode;
import com.llmwiki.domain.graph.repository.KgEdgeRepository;
import com.llmwiki.domain.graph.repository.KgNodeRepository;
import com.llmwiki.domain.graph.repository.KgVectorRepository;
import com.llmwiki.domain.page.entity.Page;
import com.llmwiki.domain.page.repository.PageLinkRepository;
import com.llmwiki.domain.page.repository.PageRepository;
import com.llmwiki.domain.page.repository.PageTagRepository;
import com.llmwiki.domain.processing.repository.ProcessingLogRepository;
import com.llmwiki.domain.sync.entity.RawDocument;
import com.llmwiki.domain.sync.repository.RawDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineServiceTest {

    @Mock RawDocumentRepository rawDocRepo;
    @Mock ProcessingLogRepository procLogRepo;
    @Mock KgNodeRepository kgNodeRepo;
    @Mock KgEdgeRepository kgEdgeRepo;
    @Mock KgVectorRepository kgVectorRepo;
    @Mock PageRepository pageRepo;
    @Mock PageLinkRepository pageLinkRepo;
    @Mock PageTagRepository pageTagRepo;
    @Mock ApprovalQueueRepository approvalQueueRepo;
    @Mock SystemConfigRepository configRepo;
    @Mock AiApiClient aiClient;
    @Mock EmbeddingClient embeddingClient;

    @InjectMocks
    PipelineService pipelineService;

    UUID rawDocId;
    RawDocument doc;
    ScoreResult scoreResult;
    ExtractionResult entities;
    ExtractionResult concepts;

    @BeforeEach
    void setUp() {
        rawDocId = UUID.randomUUID();
        doc = RawDocument.builder()
                .id(rawDocId).sourceId("src-1").sourceName("test-wiki")
                .title("Java Programming").content("Java is a programming language.")
                .contentHash("abc123").build();

        scoreResult = new ScoreResult();
        scoreResult.setOverallScore(new BigDecimal("75.0"));
        scoreResult.setReason("Good quality");
        scoreResult.setScores(Map.of("relevance", 80, "completeness", 70));
        scoreResult.setKeyEntities(List.of("Java"));
        scoreResult.setSuggestedTags(List.of("programming", "language"));

        entities = new ExtractionResult();
        entities.setEntities(List.of(
                new EntityInfo("Java", "TECH", "Programming language")));
        entities.setConcepts(Collections.emptyList());

        concepts = new ExtractionResult();
        concepts.setEntities(Collections.emptyList());
        concepts.setConcepts(List.of(
                new ConceptInfo("OOP", "Object-Oriented", List.of("Java"))));
    }

    @Test
    void processDocument_shouldSkipWhenScoreBelowThreshold() {
        ScoreResult lowScore = new ScoreResult();
        lowScore.setOverallScore(new BigDecimal("30.0"));
        lowScore.setReason("Low quality");

        when(rawDocRepo.findById(rawDocId)).thenReturn(Optional.of(doc));
        when(configRepo.findByKey("scoring.threshold")).thenReturn(Optional.empty());
        when(aiClient.score(doc.getContent())).thenReturn(lowScore);
        when(procLogRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        pipelineService.processDocument(rawDocId);

        verify(aiClient).score(doc.getContent());
        verify(pageRepo, never()).save(any());
        verify(kgNodeRepo, never()).save(any());
        verify(procLogRepo, atLeastOnce()).save(any());
    }

    @Test
    void processDocument_shouldProcessWhenScoreAboveThreshold() {
        when(rawDocRepo.findById(rawDocId)).thenReturn(Optional.of(doc));
        when(configRepo.findByKey("scoring.threshold")).thenReturn(Optional.empty());
        when(aiClient.score(doc.getContent())).thenReturn(scoreResult);
        when(aiClient.extractEntities(doc.getContent())).thenReturn(entities);
        when(aiClient.extractConcepts(doc.getContent())).thenReturn(concepts);
        when(kgNodeRepo.findByNameAndNodeType("Java", NodeType.ENTITY)).thenReturn(Optional.empty());
        when(kgNodeRepo.findByNameAndNodeType("OOP", NodeType.CONCEPT)).thenReturn(Optional.empty());
        when(kgNodeRepo.save(any(KgNode.class))).thenAnswer(i -> {
            KgNode n = i.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });
        when(embeddingClient.embed(any())).thenReturn(new float[1536]);
        when(pageRepo.findBySlug(any())).thenReturn(Optional.empty());
        when(pageRepo.save(any(Page.class))).thenAnswer(i -> {
            Page p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(procLogRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(approvalQueueRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        pipelineService.processDocument(rawDocId);

        verify(aiClient).score(doc.getContent());
        verify(aiClient).extractEntities(doc.getContent());
        verify(aiClient).extractConcepts(doc.getContent());
        verify(kgNodeRepo, atLeast(2)).save(any(KgNode.class));
        verify(pageRepo).save(any(Page.class));
        verify(approvalQueueRepo).save(any());
    }

    @Test
    void processDocument_shouldUseExistingKgNodes() {
        KgNode existingNode = KgNode.builder()
                .id(UUID.randomUUID()).name("Java").nodeType(NodeType.ENTITY).build();

        when(rawDocRepo.findById(rawDocId)).thenReturn(Optional.of(doc));
        when(configRepo.findByKey("scoring.threshold")).thenReturn(Optional.empty());
        when(aiClient.score(doc.getContent())).thenReturn(scoreResult);
        when(aiClient.extractEntities(doc.getContent())).thenReturn(entities);
        when(aiClient.extractConcepts(doc.getContent())).thenReturn(concepts);
        when(kgNodeRepo.findByNameAndNodeType("Java", NodeType.ENTITY)).thenReturn(Optional.of(existingNode));
        when(kgNodeRepo.findByNameAndNodeType("OOP", NodeType.CONCEPT)).thenReturn(Optional.empty());
        when(kgNodeRepo.save(any(KgNode.class))).thenAnswer(i -> {
            KgNode n = i.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });
        when(embeddingClient.embed(any())).thenReturn(new float[1536]);
        when(pageRepo.findBySlug(any())).thenReturn(Optional.empty());
        when(pageRepo.save(any(Page.class))).thenAnswer(i -> {
            Page p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(procLogRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(approvalQueueRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        pipelineService.processDocument(rawDocId);

        ArgumentCaptor<KgNode> nodeCaptor = ArgumentCaptor.forClass(KgNode.class);
        verify(kgNodeRepo, atLeastOnce()).save(nodeCaptor.capture());
        long entitySaves = nodeCaptor.getAllValues().stream()
                .filter(n -> n.getNodeType() == NodeType.ENTITY).count();
        assertEquals(0, entitySaves);
    }

    @Test
    void processDocument_shouldCreateEdgesBetweenConceptsAndEntities() {
        KgNode entityNode = KgNode.builder()
                .id(UUID.randomUUID()).name("Java").nodeType(NodeType.ENTITY).build();
        KgNode conceptNode = KgNode.builder()
                .id(UUID.randomUUID()).name("OOP").nodeType(NodeType.CONCEPT).build();

        when(rawDocRepo.findById(rawDocId)).thenReturn(Optional.of(doc));
        when(configRepo.findByKey("scoring.threshold")).thenReturn(Optional.empty());
        when(aiClient.score(doc.getContent())).thenReturn(scoreResult);
        when(aiClient.extractEntities(doc.getContent())).thenReturn(entities);
        when(aiClient.extractConcepts(doc.getContent())).thenReturn(concepts);
        when(kgNodeRepo.findByNameAndNodeType("Java", NodeType.ENTITY)).thenReturn(Optional.of(entityNode));
        when(kgNodeRepo.findByNameAndNodeType("OOP", NodeType.CONCEPT)).thenReturn(Optional.of(conceptNode));
        when(kgEdgeRepo.findBySourceNodeId(conceptNode.getId())).thenReturn(List.of());
        when(kgEdgeRepo.save(any(KgEdge.class))).thenAnswer(i -> i.getArgument(0));
        when(pageRepo.findBySlug(any())).thenReturn(Optional.empty());
        when(pageRepo.save(any(Page.class))).thenAnswer(i -> {
            Page p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(procLogRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(approvalQueueRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        pipelineService.processDocument(rawDocId);

        verify(kgEdgeRepo).save(any(KgEdge.class));
    }

    @Test
    void processDocument_shouldNotCreateDuplicateEdges() {
        KgNode entityNode = KgNode.builder()
                .id(UUID.randomUUID()).name("Java").nodeType(NodeType.ENTITY).build();
        KgNode conceptNode = KgNode.builder()
                .id(UUID.randomUUID()).name("OOP").nodeType(NodeType.CONCEPT).build();
        KgEdge existingEdge = KgEdge.builder()
                .id(UUID.randomUUID()).sourceNodeId(conceptNode.getId())
                .targetNodeId(entityNode.getId()).edgeType(EdgeType.RELATED_TO)
                .weight(BigDecimal.valueOf(0.5)).build();

        when(rawDocRepo.findById(rawDocId)).thenReturn(Optional.of(doc));
        when(configRepo.findByKey("scoring.threshold")).thenReturn(Optional.empty());
        when(aiClient.score(doc.getContent())).thenReturn(scoreResult);
        when(aiClient.extractEntities(doc.getContent())).thenReturn(entities);
        when(aiClient.extractConcepts(doc.getContent())).thenReturn(concepts);
        when(kgNodeRepo.findByNameAndNodeType("Java", NodeType.ENTITY)).thenReturn(Optional.of(entityNode));
        when(kgNodeRepo.findByNameAndNodeType("OOP", NodeType.CONCEPT)).thenReturn(Optional.of(conceptNode));
        when(kgEdgeRepo.findBySourceNodeId(conceptNode.getId())).thenReturn(List.of(existingEdge));
        when(pageRepo.findBySlug(any())).thenReturn(Optional.empty());
        when(pageRepo.save(any(Page.class))).thenAnswer(i -> {
            Page p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(procLogRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(approvalQueueRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        pipelineService.processDocument(rawDocId);

        verify(kgEdgeRepo, never()).save(any(KgEdge.class));
    }

    @Test
    void processDocument_shouldAutoSubmitForApproval() {
        when(rawDocRepo.findById(rawDocId)).thenReturn(Optional.of(doc));
        when(configRepo.findByKey("scoring.threshold")).thenReturn(Optional.empty());
        when(aiClient.score(doc.getContent())).thenReturn(scoreResult);
        when(aiClient.extractEntities(doc.getContent())).thenReturn(entities);
        when(aiClient.extractConcepts(doc.getContent())).thenReturn(concepts);
        when(kgNodeRepo.findByNameAndNodeType(any(), any())).thenReturn(Optional.empty());
        when(kgNodeRepo.save(any(KgNode.class))).thenAnswer(i -> {
            KgNode n = i.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });
        when(embeddingClient.embed(any())).thenReturn(new float[1536]);
        when(pageRepo.findBySlug(any())).thenReturn(Optional.empty());
        when(pageRepo.save(any(Page.class))).thenAnswer(i -> {
            Page p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(procLogRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(approvalQueueRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        pipelineService.processDocument(rawDocId);

        ArgumentCaptor<com.llmwiki.domain.approval.entity.ApprovalQueue> captor =
                ArgumentCaptor.forClass(com.llmwiki.domain.approval.entity.ApprovalQueue.class);
        verify(approvalQueueRepo).save(captor.capture());
        assertEquals(ApprovalStatus.PENDING.name(), captor.getValue().getStatus());
        assertEquals(ApprovalAction.CREATE.name(), captor.getValue().getAction());
    }

    @Test
    void processDocument_shouldThrowWhenDocNotFound() {
        when(rawDocRepo.findById(rawDocId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> pipelineService.processDocument(rawDocId));
    }

    @Test
    void generateUniqueSlug_shouldAppendSuffixForDuplicates() {
        when(rawDocRepo.findById(rawDocId)).thenReturn(Optional.of(doc));
        when(configRepo.findByKey("scoring.threshold")).thenReturn(Optional.empty());
        when(aiClient.score(doc.getContent())).thenReturn(scoreResult);
        when(aiClient.extractEntities(doc.getContent())).thenReturn(entities);
        when(aiClient.extractConcepts(doc.getContent())).thenReturn(concepts);
        when(kgNodeRepo.findByNameAndNodeType(any(), any())).thenReturn(Optional.empty());
        when(kgNodeRepo.save(any(KgNode.class))).thenAnswer(i -> {
            KgNode n = i.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });
        when(embeddingClient.embed(any())).thenReturn(new float[1536]);
        when(pageRepo.findBySlug("java-programming")).thenReturn(Optional.of(Page.builder().build()));
        when(pageRepo.findBySlug("java-programming-1")).thenReturn(Optional.empty());
        when(pageRepo.save(any(Page.class))).thenAnswer(i -> {
            Page p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(procLogRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(approvalQueueRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        pipelineService.processDocument(rawDocId);

        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(pageRepo).save(pageCaptor.capture());
        assertEquals("java-programming-1", pageCaptor.getValue().getSlug());
    }
}
