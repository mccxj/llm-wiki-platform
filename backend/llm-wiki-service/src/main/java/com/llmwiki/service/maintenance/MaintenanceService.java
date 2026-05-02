package com.llmwiki.service.maintenance;

import com.llmwiki.domain.graph.entity.KgEdge;
import com.llmwiki.domain.graph.entity.KgNode;
import com.llmwiki.domain.graph.repository.KgEdgeRepository;
import com.llmwiki.domain.graph.repository.KgNodeRepository;
import com.llmwiki.domain.page.entity.Page;
import com.llmwiki.domain.page.repository.PageRepository;
import com.llmwiki.domain.processing.repository.ProcessingLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 维护服务：孤儿检测、过时检测、重复检测、矛盾检测
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceService {

    private static final double DUPLICATE_THRESHOLD = 0.85;

    private final KgNodeRepository kgNodeRepo;
    private final KgEdgeRepository kgEdgeRepo;
    private final PageRepository pageRepo;
    private final ProcessingLogRepository procLogRepo;

    /**
     * 查找孤儿页面（没有入链的页面）
     */
    public List<Page> findOrphans() {
        List<UUID> connectedPageIds = kgEdgeRepo.findConnectedPageIds();
        return pageRepo.findOrphanPages(connectedPageIds);
    }

    /**
     * 查找过时页面（超过days天未更新）
     */
    public List<Page> findStalePages(int days) {
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        return pageRepo.findAll().stream()
                .filter(p -> p.getUpdatedAt() != null && p.getUpdatedAt().isBefore(cutoff))
                .collect(Collectors.toList());
    }

    /**
     * 查找可能重复的页面（使用Jaro-Winkler相似度）
     * 相似度超过0.85的页面被归为同一组
     */
    public List<DuplicateGroup> findDuplicates() {
        List<Page> allPages = pageRepo.findAll();
        JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();

        // Track which pages have been grouped to avoid duplicates in output
        Set<UUID> grouped = new HashSet<>();
        List<DuplicateGroup> result = new ArrayList<>();

        for (int i = 0; i < allPages.size(); i++) {
            Page pageA = allPages.get(i);
            if (grouped.contains(pageA.getId())) continue;

            List<Page> group = new ArrayList<>();
            group.add(pageA);
            double maxSimilarity = 1.0;

            for (int j = i + 1; j < allPages.size(); j++) {
                Page pageB = allPages.get(j);
                if (grouped.contains(pageB.getId())) continue;

                String titleA = pageA.getTitle().toLowerCase().trim();
                String titleB = pageB.getTitle().toLowerCase().trim();

                Double sim = similarity.apply(titleA, titleB);
                if (sim != null && sim > DUPLICATE_THRESHOLD) {
                    group.add(pageB);
                    grouped.add(pageB.getId());
                    if (sim < maxSimilarity) {
                        maxSimilarity = sim;
                    }
                }
            }

            if (group.size() > 1) {
                grouped.add(pageA.getId());
                result.add(new DuplicateGroup(group, maxSimilarity));
            }
        }

        return result;
    }

    /**
     * 查找矛盾页面（标记为contested）
     */
    public List<Page> findContradictions() {
        return pageRepo.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getContested()))
                .collect(Collectors.toList());
    }

    /**
     * 查找建议拆分的页面（content长度超过10000字符，约200行）
     */
    public List<Page> findSplitSuggestions() {
        return pageRepo.findAll().stream()
                .filter(p -> p.getContent() != null && p.getContent().length() > 10000)
                .collect(Collectors.toList());
    }

    /**
     * 索引一致性检查：查找有页面但无对应节点关联的页面
     */
    public Map<String, Object> indexConsistencyCheck() {
        List<KgNode> allNodes = kgNodeRepo.findAll();
        Set<UUID> pageIdsWithNodes = new HashSet<>();
        for (KgNode n : allNodes) {
            if (n.getPageId() != null) {
                pageIdsWithNodes.add(n.getPageId());
            }
        }

        // Pages without any associated nodes
        List<String> orphanPages = new ArrayList<>();
        for (Page p : pageRepo.findAll()) {
            if (!pageIdsWithNodes.contains(p.getId())) {
                orphanPages.add(p.getTitle());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalPages", pageRepo.count());
        result.put("pagesWithoutNodes", orphanPages.size());
        result.put("orphanPages", orphanPages);
        return result;
    }

    /**
     * 生成维护报告
     */
    public MaintenanceReport generateReport() {
        MaintenanceReport report = new MaintenanceReport();
        report.setGeneratedAt(Instant.now());
        report.setTotalPages(pageRepo.count());
        report.setOrphanCount(findOrphans().size());
        report.setStaleCount(findStalePages(30).size());
        List<DuplicateGroup> dupGroups = findDuplicates();
        report.setDuplicateGroups(dupGroups.size());
        report.setContradictionCount(findContradictions().size());
        report.setOrphans(findOrphans());
        report.setStalePages(findStalePages(30));
        report.setDuplicates(dupGroups);
        report.setContradictions(findContradictions());
        return report;
    }
}
