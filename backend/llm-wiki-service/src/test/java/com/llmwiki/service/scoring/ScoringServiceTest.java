package com.llmwiki.service.scoring;

import com.llmwiki.adapter.api.AiApiClient;
import com.llmwiki.adapter.dto.ScoreResult;
import com.llmwiki.domain.config.entity.SystemConfig;
import com.llmwiki.domain.config.repository.SystemConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoringServiceTest {

    @Mock
    private AiApiClient aiClient;

    @Mock
    private SystemConfigRepository configRepo;

    @InjectMocks
    private ScoringService scoringService;

    @Test
    void scoreDocument_shouldReturnZeroForNullContent() {
        ScoreResult result = scoringService.scoreDocument(null);
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getOverallScore());
    }

    @Test
    void scoreDocument_shouldReturnZeroForBlankContent() {
        ScoreResult result = scoringService.scoreDocument("   ");
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getOverallScore());
    }

    @Test
    void scoreDocument_shouldCallAiAndCalculateWeightedScore() {
        when(configRepo.findByKey("scoring.weights")).thenReturn(Optional.empty());

        Map<String, Integer> scores = new HashMap<>();
        scores.put("information_density", 8);
        scores.put("entity_richness", 7);
        scores.put("knowledge_independence", 6);
        scores.put("structure_integrity", 9);
        scores.put("timeliness", 5);

        ScoreResult aiResult = ScoreResult.builder()
                .scores(scores)
                .overallScore(BigDecimal.ZERO)
                .reason("Good document")
                .build();

        when(aiClient.score("test content")).thenReturn(aiResult);

        ScoreResult result = scoringService.scoreDocument("test content");

        assertNotNull(result);
        assertNotNull(result.getOverallScore());
        assertTrue(result.getOverallScore().compareTo(BigDecimal.ZERO) > 0);
        verify(aiClient).score("test content");
    }

    @Test
    void calculateWeightedScore_shouldReturnZeroForNullScores() {
        BigDecimal result = scoringService.calculateWeightedScore(null);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateWeightedScore_shouldReturnZeroForEmptyScores() {
        BigDecimal result = scoringService.calculateWeightedScore(new HashMap<>());
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateWeightedScore_shouldCalculateCorrectly() {
        Map<String, Integer> scores = new HashMap<>();
        scores.put("information_density", 10);
        scores.put("entity_richness", 0);
        scores.put("knowledge_independence", 0);
        scores.put("structure_integrity", 0);
        scores.put("timeliness", 0);

        BigDecimal result = scoringService.calculateWeightedScore(scores);
        assertEquals(new BigDecimal("3.00"), result);
    }

    @Test
    void calculateWeightedScore_shouldUseDefaultWeights() {
        Map<String, Integer> scores = new HashMap<>();
        scores.put("information_density", 10);
        scores.put("entity_richness", 10);
        scores.put("knowledge_independence", 10);
        scores.put("structure_integrity", 10);
        scores.put("timeliness", 10);

        BigDecimal result = scoringService.calculateWeightedScore(scores);
        assertEquals(new BigDecimal("10.00"), result);
    }

    @Test
    void passesThreshold_shouldReturnFalseForNullResult() {
        assertFalse(scoringService.passesThreshold(null));
    }

    @Test
    void passesThreshold_shouldReturnFalseForNullScore() {
        ScoreResult result = ScoreResult.builder().overallScore(null).build();
        assertFalse(scoringService.passesThreshold(result));
    }

    @Test
    void passesThreshold_shouldReturnTrueWhenAboveThreshold() {
        when(configRepo.findByKey("scoring.threshold")).thenReturn(Optional.empty());

        ScoreResult result = ScoreResult.builder()
                .overallScore(new BigDecimal("7.0"))
                .build();
        assertTrue(scoringService.passesThreshold(result));
    }

    @Test
    void passesThreshold_shouldReturnFalseWhenBelowThreshold() {
        when(configRepo.findByKey("scoring.threshold")).thenReturn(Optional.empty());

        ScoreResult result = ScoreResult.builder()
                .overallScore(new BigDecimal("3.0"))
                .build();
        assertFalse(scoringService.passesThreshold(result));
    }

    @Test
    void passesThreshold_shouldUseConfiguredThreshold() {
        SystemConfig config = new SystemConfig();
        config.setKey("scoring.threshold");
        config.setValue("8.0");
        when(configRepo.findByKey("scoring.threshold")).thenReturn(Optional.of(config));

        ScoreResult result = ScoreResult.builder()
                .overallScore(new BigDecimal("7.5"))
                .build();
        assertFalse(scoringService.passesThreshold(result));
    }

    @Test
    void passesDimensionThresholds_shouldReturnFalseForNullScores() {
        assertFalse(scoringService.passesDimensionThresholds(null));
    }

    @Test
    void passesDimensionThresholds_shouldReturnFalseForEmptyScores() {
        assertFalse(scoringService.passesDimensionThresholds(new HashMap<>()));
    }

    @Test
    void passesDimensionThresholds_shouldReturnTrueWhenAllAboveDefault() {
        when(configRepo.findByKey("scoring.threshold.relevance")).thenReturn(Optional.empty());
        when(configRepo.findByKey("scoring.threshold.accuracy")).thenReturn(Optional.empty());

        Map<String, Integer> scores = new HashMap<>();
        scores.put("relevance", 8);
        scores.put("accuracy", 7);
        assertTrue(scoringService.passesDimensionThresholds(scores));
    }

    @Test
    void passesDimensionThresholds_shouldReturnFalseWhenOneBelowDefault() {
        when(configRepo.findByKey("scoring.threshold.accuracy")).thenReturn(Optional.empty());

        Map<String, Integer> scores = new HashMap<>();
        scores.put("accuracy", 3);
        assertFalse(scoringService.passesDimensionThresholds(scores));
    }

    @Test
    void getThreshold_shouldReturnDefaultWhenNotConfigured() {
        when(configRepo.findByKey("scoring.threshold")).thenReturn(Optional.empty());

        BigDecimal threshold = scoringService.getThreshold();
        assertEquals(new BigDecimal("5.0"), threshold);
    }

    @Test
    void getThreshold_shouldReturnConfiguredValue() {
        SystemConfig config = new SystemConfig();
        config.setKey("scoring.threshold");
        config.setValue("7.5");
        when(configRepo.findByKey("scoring.threshold")).thenReturn(Optional.of(config));

        BigDecimal threshold = scoringService.getThreshold();
        assertEquals(new BigDecimal("7.5"), threshold);
    }

    @Test
    void getThreshold_shouldFallbackToDefaultOnInvalidValue() {
        SystemConfig config = new SystemConfig();
        config.setKey("scoring.threshold");
        config.setValue("invalid");
        when(configRepo.findByKey("scoring.threshold")).thenReturn(Optional.of(config));

        BigDecimal threshold = scoringService.getThreshold();
        assertEquals(new BigDecimal("5.0"), threshold);
    }

    @Test
    void getScoreWeights_shouldReturnDefaultsWhenNotConfigured() {
        when(configRepo.findByKey("scoring.weights")).thenReturn(Optional.empty());

        Map<String, BigDecimal> weights = scoringService.getScoreWeights();
        assertEquals(new BigDecimal("0.30"), weights.get("information_density"));
        assertEquals(new BigDecimal("0.25"), weights.get("entity_richness"));
        assertEquals(new BigDecimal("0.20"), weights.get("knowledge_independence"));
        assertEquals(new BigDecimal("0.15"), weights.get("structure_integrity"));
        assertEquals(new BigDecimal("0.10"), weights.get("timeliness"));
    }

    @Test
    void getScoreWeights_shouldParseConfiguredWeights() {
        SystemConfig config = new SystemConfig();
        config.setKey("scoring.weights");
        config.setValue("information_density:0.5,entity_richness:0.3,knowledge_independence:0.1,structure_integrity:0.05,timeliness:0.05");
        when(configRepo.findByKey("scoring.weights")).thenReturn(Optional.of(config));

        Map<String, BigDecimal> weights = scoringService.getScoreWeights();
        assertEquals(new BigDecimal("0.5"), weights.get("information_density"));
        assertEquals(new BigDecimal("0.3"), weights.get("entity_richness"));
    }
}
