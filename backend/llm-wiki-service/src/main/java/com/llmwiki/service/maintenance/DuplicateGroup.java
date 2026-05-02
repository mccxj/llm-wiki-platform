package com.llmwiki.service.maintenance;

import com.llmwiki.domain.page.entity.Page;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a group of potentially duplicate pages with their similarity score.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateGroup {
    private List<Page> pages;
    private double similarity;
}
