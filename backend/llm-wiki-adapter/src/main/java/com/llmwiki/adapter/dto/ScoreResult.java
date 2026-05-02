package com.llmwiki.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScoreResult {
    private Map<String, Integer> scores;
    private BigDecimal overallScore;
    private String reason;
    private List<String> keyEntities;
    private List<String> suggestedTags;
}
