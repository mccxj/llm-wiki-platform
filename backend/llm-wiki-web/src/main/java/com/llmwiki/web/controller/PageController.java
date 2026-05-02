package com.llmwiki.web.controller;

import com.llmwiki.domain.page.entity.Page;
import com.llmwiki.domain.page.entity.PageLink;
import com.llmwiki.domain.page.repository.PageLinkRepository;
import com.llmwiki.domain.page.repository.PageRepository;
import com.llmwiki.domain.processing.entity.ProcessingLog;
import com.llmwiki.domain.processing.repository.ProcessingLogRepository;
import com.llmwiki.common.enums.PageType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 页面管理控制器
 */
@RestController
@RequestMapping("/api/pages")
@RequiredArgsConstructor
public class PageController {

    private final PageRepository pageRepo;
    private final PageLinkRepository pageLinkRepo;
    private final ProcessingLogRepository procLogRepo;

    /**
     * 获取页面列表
     */
    @GetMapping
    public ResponseEntity<List<Page>> list(
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Page> pages;
        if (!"ALL".equals(status) && !"ALL".equals(type)) {
            pages = pageRepo.findByStatusAndPageType(status, PageType.valueOf(type), pageable);
        } else if (!"ALL".equals(status)) {
            pages = pageRepo.findByStatus(status, pageable);
        } else if (!"ALL".equals(type)) {
            pages = pageRepo.findByPageType(PageType.valueOf(type), pageable);
        } else {
            pages = pageRepo.findAll(pageable).getContent();
        }
        return ResponseEntity.ok(pages);
    }

    /**
     * 获取页面详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Page> getById(@PathVariable UUID id) {
        return pageRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取页面的交叉链接
     */
    @GetMapping("/{id}/links")
    public ResponseEntity<List<PageLink>> getLinks(@PathVariable UUID id) {
        return ResponseEntity.ok(pageLinkRepo.findBySourcePageId(id));
    }

    /**
     * 获取页面处理历史
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<List<ProcessingLog>> getHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(procLogRepo.findByRawDocumentId(id));
    }

    /**
     * 软删除页面（设置为ARCHIVED状态）
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return pageRepo.findById(id).<ResponseEntity<Void>>map(page -> {
            page.setStatus(com.llmwiki.common.enums.PageStatus.ARCHIVED);
            pageRepo.save(page);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
