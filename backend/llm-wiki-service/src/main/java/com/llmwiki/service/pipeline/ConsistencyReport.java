package com.llmwiki.service.pipeline;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 一致性检查报告
 */
@Data
@Builder
public class ConsistencyReport {
    private boolean passed;
    private List<String> issues;
    private int entityCount;
    private int linkedPagesCount;
}
