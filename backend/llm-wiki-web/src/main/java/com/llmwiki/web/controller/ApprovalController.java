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

    @GetMapping("/pending")
    public ResponseEntity<List<ApprovalQueue>> pending() {
        return ResponseEntity.ok(approvalService.listPending());
    }

    @GetMapping
    public ResponseEntity<List<ApprovalQueue>> list(@RequestParam(defaultValue = "PENDING") String status) {
        return ResponseEntity.ok(approvalService.listByStatus(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApprovalQueue> getById(@PathVariable UUID id) {
        return approvalRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
