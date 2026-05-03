package com.llmwiki.service.pipeline;

import com.llmwiki.adapter.api.AiApiClient;
import com.llmwiki.adapter.api.EmbeddingClient;
import com.llmwiki.adapter.extraction.MultiPassExtractor;
import com.llmwiki.adapter.dto.ExtractionResult;
import com.llmwiki.adapter.dto.ExtractionResult.ConceptInfo;
import com.llmwiki.adapter.dto.ExtractionResult.EntityInfo;
import com.llmwiki.adapter.dto.ScoreResult;
import com.llmwiki.common.enums.*;
import com.llmwiki.domain.approval.repository.ApprovalQueueRepository;
import com.llmwiki.domain.graph.repository.KgEdgeRepository;
import com.llmwiki.domain.graph.repository.KgNodeRepository;
import com.llmwiki.domain.graph.repository.KgVectorRepository;
import com.llmwiki.domain.page.repository.PageLinkRepository;
import com.llmwiki.domain.page.repository.PageRepository;
import com.llmwiki.domain.page.repository.PageTagRepository;
import com.llmwiki.domain.pipeline.repository.DeadLetterQueueRepository;
import com.llmwiki.domain.processing.repository.ProcessingLogRepository;
import com.llmwiki.domain.sync.entity.RawDocument;
import com.llmwiki.domain.sync.repository.RawDocumentRepository;
import com.llmwiki.domain.config.repository.SystemConfigRepository;
import com.llmwiki.service.scoring.ScoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PipelineServiceTest {

    @Mock private RawDocumentRepository rawDocRepo;
    @Mock private ProcessingLogRepository procLogRepo;
    @Mock private KgNodeRepository kgNodeRepo;
    @Mock private KgEdgeRepository kgEdgeRepo;
    @Mock private KgVectorRepository kgVectorRepo;
    @Mock private PageRepository pageRepo;
    @Mock private PageLinkRepository pageLinkRepo;
    @Mock private PageTagRepository pageTagRepo;
    @Mock private ApprovalQueueRepository approvalQueueRepo;
    @Mock private DeadLetterQueueRepository deadLetterQueueRepo;
    @Mock private SystemConfigRepository configRepo;
    @Mock private AiApiClient aiClient;
    @Mock private EmbeddingClient embeddingClient;
    @Mock private ScoringService scoringService;
    @Mock private MultiPassExtractor multiPassExtractor;

    @InjectMocks
    private PipelineService pipelineService;

    private RawDocument testDoc;

    @BeforeEach
    void setUp() {
        testDoc = new RawDocument();
        testDoc.setId(UUID.randomUUID());
        testDoc.setTitle("Test Document");
        testDoc.setContent("Test content with some entities");
        testDoc.setSourceUrl("http://example.com/doc");

        when(configRepo.findByKey("pipeline.max.retries"))
                .thenReturn(Optional.empty());  // uses default 3
    }

    @Test
    void shouldScoreDocument() {
        ScoreResult scoreResult = new ScoreResult();
        scoreResult.setOverallScore(BigDecimal.valueOf(8.0));
        scoreResult.setScores(Map.of("information_density", 8, "entity_richness", 7,
                "knowledge_independence", 9, "structure_integrity", 8, "timeliness", 7));
        scoreResult.setReason("Good document");
        scoreResult.setKeyEntities(List.of("Entity1"));
        scoreResult.setSuggestedTags(List.of("tag1"));

        when(rawDocRepo.findById(any())).thenReturn(Optional.of(testDoc));
        when(scoringService.scoreDocument(any())).thenReturn(scoreResult);
        when(scoringService.passesThreshold(any())).thenReturn(false);
        when(scoringService.getThreshold()).thenReturn(BigDecimal.valueOf(5.0));

        pipelineService.processDocument(testDoc.getId());

        verify(scoringService).scoreDocument(testDoc.getContent());
    }

    @Test
    void shouldExtractEntitiesUsingMultiPass() {
        ExtractionResult emptyConcepts = new ExtractionResult();
        emptyConcepts.setEntities(Collections.emptyList());
        emptyConcepts.setConcepts(Collections.emptyList());

        when(rawDocRepo.findById(any())).thenReturn(Optional.of(testDoc));
        when(scoringService.passesThreshold(any())).thenReturn(true);
        when(multiPassExtractor.extractAll(any(), any())).thenReturn(List.of(
                new EntityInfo("Java", "TECH", "Programming language"),
                new EntityInfo("Spring", "TECH", "Framework")
        ));
        when(aiClient.extractConcepts(any())).thenReturn(emptyConcepts);
        when(embeddingClient.embed(any())).thenReturn(new float[1536]);
        when(kgNodeRepo.findByNameAndNodeType(any(), any())).thenReturn(Optional.empty());
        when(pageRepo.findBySlug(any())).thenReturn(Optional.empty());

        ScoreResult scoreResult = new ScoreResult();
        scoreResult.setOverallScore(BigDecimal.valueOf(8.0));
        scoreResult.setScores(Map.of("information_density", 8, "entity_richness", 7,
                "knowledge_independence", 9, "structure_integrity", 8, "timeliness", 7));
        scoreResult.setReason("Good");
        scoreResult.setKeyEntities(List.of());
        scoreResult.setSuggestedTags(List.of());
        when(scoringService.scoreDocument(any())).thenReturn(scoreResult);

        pipelineService.processDocument(testDoc.getId());

        verify(multiPassExtractor).extractAll(eq(testDoc.getContent()), any());
    }

    @Test
    void shouldExtractConcepts() {
        ExtractionResult conceptResult = new ExtractionResult();
        conceptResult.setEntities(Collections.emptyList());
        conceptResult.setConcepts(List.of(
                new ConceptInfo("OOP", "Object-oriented programming", List.of("Java"))
        ));

        when(rawDocRepo.findById(any())).thenReturn(Optional.of(testDoc));
        when(scoringService.passesThreshold(any())).thenReturn(true);
        when(multiPassExtractor.extractAll(any(), any())).thenReturn(Collections.emptyList());
        when(aiClient.extractConcepts(any())).thenReturn(conceptResult);
        when(embeddingClient.embed(any())).thenReturn(new float[1536]);
        when(kgNodeRepo.findByNameAndNodeType(any(), any())).thenReturn(Optional.empty());

        ScoreResult scoreResult = new ScoreResult();
        scoreResult.setOverallScore(BigDecimal.valueOf(8.0));
        scoreResult.setScores(Map.of("information_density", 8, "entity_richness", 7,
                "knowledge_independence", 9, "structure_integrity", 8, "timeliness", 7));
        scoreResult.setReason("Good");
        scoreResult.setKeyEntities(List.of());
        scoreResult.setSuggestedTags(List.of());
        when(scoringService.scoreDocument(any())).thenReturn(scoreResult);

        pipelineService.processDocument(testDoc.getId());

        verify(aiClient).extractConcepts(testDoc.getContent());
    }

    @Test
    void shouldCreateKnowledgeGraphNodes() {
        ExtractionResult emptyConcepts = new ExtractionResult();
        emptyConcepts.setEntities(Collections.emptyList());
        emptyConcepts.setConcepts(Collections.emptyList());

        when(rawDocRepo.findById(any())).thenReturn(Optional.of(testDoc));
        when(scoringService.passesThreshold(any())).thenReturn(true);
        when(multiPassExtractor.extractAll(any(), any())).thenReturn(List.of(
                new EntityInfo("Python", "TECH", "Language")
        ));
        when(aiClient.extractConcepts(any())).thenReturn(emptyConcepts);
        when(embeddingClient.embed(any())).thenReturn(new float[1536]);
        when(kgNodeRepo.findByNameAndNodeType(any(), any())).thenReturn(Optional.empty());
        when(pageRepo.findBySlug(any())).thenReturn(Optional.empty());

        ScoreResult scoreResult = new ScoreResult();
        scoreResult.setOverallScore(BigDecimal.valueOf(8.0));
        scoreResult.setScores(Map.of("information_density", 8, "entity_richness", 7,
                "knowledge_independence", 9, "structure_integrity", 8, "timeliness", 7));
        scoreResult.setReason("Good");
        scoreResult.setKeyEntities(List.of());
        scoreResult.setSuggestedTags(List.of());
        when(scoringService.scoreDocument(any())).thenReturn(scoreResult);

        pipelineService.processDocument(testDoc.getId());

        verify(kgNodeRepo, atLeastOnce()).save(any());
    }

    @Test
    void shouldSucceedOnRetry() {
        RawDocument doc = new RawDocument();
        doc.setId(UUID.randomUUID());
        doc.setTitle("Retry Doc");
        doc.setContent("retry content");
        doc.setSourceUrl("http://retry.com");

        when(rawDocRepo.findById(any())).thenReturn(Optional.of(doc));
        when(scoringService.scoreDocument(any()))
                .thenThrow(new RuntimeException("Temporary failure"))
                .thenReturn(new ScoreResult() {{ setOverallScore(BigDecimal.valueOf(6.0)); }});
        when(scoringService.passesThreshold(any())).thenReturn(false);
        when(scoringService.getThreshold()).thenReturn(BigDecimal.valueOf(5.0));

        pipelineService.processDocument(doc.getId());

        verify(scoringService, atLeast(2)).scoreDocument(any());
    }

    @Test
    void shouldSaveToDLQOnPersistentFailure() {
        when(rawDocRepo.findById(any())).thenReturn(Optional.of(testDoc));
        when(scoringService.scoreDocument(any())).thenThrow(new RuntimeException("Persistent failure"));

        pipelineService.processDocument(testDoc.getId());

        verify(deadLetterQueueRepo).save(any());
    }

    @Test
    void shouldSkipBelowThreshold() {
        ScoreResult lowScore = new ScoreResult();
        lowScore.setOverallScore(BigDecimal.valueOf(2.0));
        lowScore.setReason("Low quality");

        when(rawDocRepo.findById(any())).thenReturn(Optional.of(testDoc));
        when(scoringService.scoreDocument(any())).thenReturn(lowScore);
        when(scoringService.passesThreshold(any())).thenReturn(false);
        when(scoringService.getThreshold()).thenReturn(BigDecimal.valueOf(5.0));

        pipelineService.processDocument(testDoc.getId());

        verify(scoringService).scoreDocument(testDoc.getContent());
        verify(aiClient, never()).extractEntities(any());
    }
}
