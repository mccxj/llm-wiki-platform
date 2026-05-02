package com.llmwiki.service.maintenance;

import com.llmwiki.domain.page.entity.Page;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 维护报告DTO
 */
@Data
public class MaintenanceReport {
    private Instant generatedAt;
    private long totalPages;
    private int orphanCount;
    private int staleCount;
    private int duplicateGroups;
    private int contradictionCount;
    private List<Page> orphans;
    private List<Page> stalePages;
    private List<DuplicateGroup> duplicates;
    private List<Page> contradictions;
}
