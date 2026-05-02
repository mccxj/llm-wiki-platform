package com.llmwiki.service.scoring;

import com.llmwiki.adapter.api.AiApiClient;
import com.llmwiki.adapter.dto.ScoreResult;
import com.llmwiki.domain.config.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 评分服务：调用AI对文档进行多维度评分，计算加权分数，并判断是否通过阈值。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScoringService {

    private final AiApiClient aiClient;
    private final SystemConfigRepository configRepo;

    private static final String THRESHOLD_KEY = "scoring.threshold";
    private static final String WEIGHTS_KEY = "scoring.weights";
    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("5.0");

    /**
     * 对文档内容进行AI评分，并计算加权总分。
     *
     * @param content 文档内容
     * @return 评分结果（overallScore 为加权计算后的分数）
     */
    public ScoreResult scoreDocument(String content) {
        if (content == null || content.isBlank()) {
            log.warn("Empty content provided for scoring");
            return ScoreResult.builder()
                    .overallScore(BigDecimal.ZERO)
                    .reason("内容为空")
                    .build();
        }
        ScoreResult result = aiClient.score(content);
        // 计算加权分数并覆盖 overallScore
        BigDecimal weightedScore = calculateWeightedScore(result.getScores());
        result.setOverallScore(weightedScore);
        return result;
    }

    /**
     * 根据各维度分数和权重计算加权总分。
     *
     * @param scores 各维度分数映射
     * @return 加权总分（0-10）
     */
    public BigDecimal calculateWeightedScore(Map<String, Integer> scores) {
        if (scores == null || scores.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Map<String, BigDecimal> weights = getScoreWeights();
        BigDecimal weightedSum = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            BigDecimal weight = weights.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            weightedSum = weightedSum.add(BigDecimal.valueOf(entry.getValue()).multiply(weight));
        }
        return weightedSum.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 判断评分结果是否通过阈值。
     *
     * @param result 评分结果
     * @return true 如果通过阈值
     */
    public boolean passesThreshold(ScoreResult result) {
        if (result == null || result.getOverallScore() == null) {
            return false;
        }
        BigDecimal threshold = configRepo.findByKey(THRESHOLD_KEY)
                .map(config -> {
                    try {
                        return new BigDecimal(config.getValue());
                    } catch (NumberFormatException e) {
                        log.warn("Invalid threshold value: {}, using default", config.getValue());
                        return DEFAULT_THRESHOLD;
                    }
                })
                .orElse(DEFAULT_THRESHOLD);

        return result.getOverallScore().compareTo(threshold) >= 0;
    }

    /**
     * 获取评分阈值。
     *
     * @return 阈值（默认 5.0，0-10 分制）
     */
    public BigDecimal getThreshold() {
        return configRepo.findByKey(THRESHOLD_KEY)
                .map(config -> {
                    try {
                        return new BigDecimal(config.getValue());
                    } catch (NumberFormatException e) {
                        log.warn("Invalid threshold value: {}, using default", config.getValue());
                        return DEFAULT_THRESHOLD;
                    }
                })
                .orElse(DEFAULT_THRESHOLD);
    }

    /**
     * 获取评分权重配置。
     * 从 system_config 中读取，格式为 "information_density:0.3,entity_richness:0.25,knowledge_independence:0.2,structure_integrity:0.15,timeliness:0.1"
     *
     * @return 权重映射
     */
    public Map<String, BigDecimal> getScoreWeights() {
        Map<String, BigDecimal> defaultWeights = new HashMap<>();
        defaultWeights.put("information_density", new BigDecimal("0.30"));
        defaultWeights.put("entity_richness", new BigDecimal("0.25"));
        defaultWeights.put("knowledge_independence", new BigDecimal("0.20"));
        defaultWeights.put("structure_integrity", new BigDecimal("0.15"));
        defaultWeights.put("timeliness", new BigDecimal("0.10"));

        return configRepo.findByKey(WEIGHTS_KEY)
                .map(config -> {
                    try {
                        Map<String, BigDecimal> weights = new HashMap<>();
                        String[] pairs = config.getValue().split(",");
                        for (String pair : pairs) {
                            String[] kv = pair.trim().split(":");
                            if (kv.length == 2) {
                                weights.put(kv[0].trim(), new BigDecimal(kv[1].trim()));
                            }
                        }
                        return weights.isEmpty() ? defaultWeights : weights;
                    } catch (Exception e) {
                        log.warn("Failed to parse score weights, using defaults", e);
                        return defaultWeights;
                    }
                })
                .orElse(defaultWeights);
    }
}
