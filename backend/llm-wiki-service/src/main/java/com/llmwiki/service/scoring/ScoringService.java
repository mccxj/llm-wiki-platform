package com.llmwiki.service.scoring;

import com.llmwiki.adapter.api.AiApiClient;
import com.llmwiki.adapter.dto.ScoreResult;
import com.llmwiki.domain.config.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 评分服务：调用AI对文档进行多维度评分，并判断是否通过阈值。
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
     * 对文档内容进行AI评分。
     *
     * @param content 文档内容
     * @return 评分结果
     */
    public ScoreResult scoreDocument(String content) {
        if (content == null || content.isBlank()) {
            log.warn("Empty content provided for scoring");
            ScoreResult emptyResult = new ScoreResult();
            emptyResult.setOverallScore(BigDecimal.ZERO);
            emptyResult.setReason("内容为空");
            return emptyResult;
        }
        return aiClient.score(content);
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
     * 获取评分权重配置。
     * 从 system_config 中读取，格式为 "relevance:0.3,completeness:0.3,accuracy:0.2,clarity:0.2"
     *
     * @return 权重映射
     */
    public Map<String, BigDecimal> getScoreWeights() {
        Map<String, BigDecimal> defaultWeights = new HashMap<>();
        defaultWeights.put("relevance", new BigDecimal("0.30"));
        defaultWeights.put("completeness", new BigDecimal("0.30"));
        defaultWeights.put("accuracy", new BigDecimal("0.20"));
        defaultWeights.put("clarity", new BigDecimal("0.20"));

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
