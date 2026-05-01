package com.llmwiki.adapter.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ScoreResult {
    private Map<String, Integer> scores;
    private BigDecimal overallScore;
    private String reason;
    private List<String> keyEntities;
    private List<String> suggestedTags;

    public ScoreResult() {}

    public Map<String, Integer> getScores() { return scores; }
    public void setScores(Map<String, Integer> scores) { this.scores = scores; }
    public BigDecimal getOverallScore() { return overallScore; }
    public void setOverallScore(BigDecimal overallScore) { this.overallScore = overallScore; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public List<String> getKeyEntities() { return keyEntities; }
    public void setKeyEntities(List<String> keyEntities) { this.keyEntities = keyEntities; }
    public List<String> getSuggestedTags() { return suggestedTags; }
    public void setSuggestedTags(List<String> suggestedTags) { this.suggestedTags = suggestedTags; }
}
