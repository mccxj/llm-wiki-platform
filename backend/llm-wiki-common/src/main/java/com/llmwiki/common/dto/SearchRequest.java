package com.llmwiki.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {
    private String query;
    private List<String> types;
    private List<String> tags;
    @Builder.Default
    private int limit = 10;
    @Builder.Default
    private int offset = 0;
}
