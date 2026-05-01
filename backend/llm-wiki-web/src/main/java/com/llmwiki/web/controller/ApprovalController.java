package com.llmwiki.web.controller;

import com.llmwiki.common.enums.ApprovalAction;
import com.llmwiki.domain.approval.entity.ApprovalQueue;
import com.llmwiki.domain.approval.repository.ApprovalQueueRepository;
import com.llmwiki.service.approval.ApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;
    private final ApprovalQueueRepository approvalRepo;

    @PostMapping("/submit/{pageId}")
    public ResponseEntity<ApprovalQueue> submit(@PathVariable UUID pageId,
                                                 @RequestBody Map<String, String> body) {
        String action = body.getOrDefault("action", "CREATE");
        String comment = body.getOrDefault("comment", "");
        return ResponseEntity.ok(approvalService.submitForApproval(pageId, ApprovalAction.valueOf(action), comment));
    }

    @PostMapping("/approve/{approvalId}")
    public ResponseEntity<ApprovalQueue> approve(@PathVariable UUID approvalId,
                                                  @RequestBody Map<String, String> body) {
        String reviewerId = body.getOrDefault("reviewerId", "admin");
        String comment = body.getOrDefault("comment", "");
        return ResponseEntity.ok(approvalService.approve(approvalId, reviewerId, comment));
    }

    @PostMapping("/reject/{approvalId}")
    public ResponseEntity<ApprovalQueue> reject(@PathVariable UUID approvalId,
                                                 @RequestBody Map<String, String> body) {
        String reviewerId = body.getOrDefault("reviewerId", "admin");
        String comment = body.getOrDefault("comment", "");
        return ResponseEntity.ok(approvalService.reject(approvalId, reviewerId, comment));
    }

    /**
     * Batch approve/reject.
     * POST /api/approvals/batch { "ids": [...], "action": "approve"|"reject", "comment": "..." }
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> batch(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) body.get("ids");
        String action = (String) body.getOrDefault("action", "approve");
        String comment = (String) body.getOrDefault("comment", "");
        String reviewerId = (String) body.getOrDefault("reviewerId", "admin");

        int success = 0;
        int failed = 0;
        for (String idStr : ids) {
            try {
                UUID id = UUID.fromString(idStr);
                if ("reject".equalsIgnoreCase(action)) {
                    approvalService.reject(id, reviewerId, comment);
                } else {
                    approvalService.approve(id, reviewerId, comment);
                }
                success++;
            } catch (Exception e) {
                failed++;
            }
        }
        return ResponseEntity.ok(Map.of("success", success, "failed", failed, "total", ids.size()));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ApprovalQueue>> pending() {
        return ResponseEntity.ok(approvalService.listPending());
    }

    @GetMapping
    public ResponseEntity<List<ApprovalQueue>> list(@RequestParam(defaultValue = "PENDING") String status) {
        return ResponseEntity.ok(approvalService.listByStatus(status));
    }

    /**
     * Get approval detail with before/after diff.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApprovalQueue> getById(@PathVariable UUID id) {
        return approvalRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get approval history for a page.
     * GET /api/approvals/history?pageId=xxx&page=0&size=20
     */
    @GetMapping("/history")
    public ResponseEntity<List<ApprovalQueue>> history(@RequestParam UUID pageId,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(approvalService.getApprovalHistory(pageId));
    }
}
