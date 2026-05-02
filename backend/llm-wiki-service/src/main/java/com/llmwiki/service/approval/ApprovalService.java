package com.llmwiki.service.approval;

import com.llmwiki.common.enums.ApprovalAction;
import com.llmwiki.common.enums.ApprovalStatus;
import com.llmwiki.common.enums.PageStatus;
import com.llmwiki.domain.approval.entity.ApprovalAudit;
import com.llmwiki.domain.approval.entity.ApprovalQueue;
import com.llmwiki.domain.approval.repository.ApprovalAuditRepository;
import com.llmwiki.domain.approval.repository.ApprovalQueueRepository;
import com.llmwiki.domain.page.entity.Page;
import com.llmwiki.domain.page.repository.PageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalService {

    private final ApprovalQueueRepository approvalRepo;
    private final ApprovalAuditRepository auditRepo;
    private final PageRepository pageRepo;

    @Transactional
    public ApprovalQueue submitForApproval(UUID pageId, ApprovalAction action, String comment) {
        Page page = pageRepo.findById(pageId)
                .orElseThrow(() -> new IllegalArgumentException("Page not found: " + pageId));

        ApprovalQueue approval = ApprovalQueue.builder()
                .pageId(pageId)
                .action(action.name())
                .comment(comment)
                .status(ApprovalStatus.PENDING.name())
                .build();

        page.setStatus(PageStatus.PENDING_APPROVAL);
        pageRepo.save(page);

        return approvalRepo.save(approval);
    }

    @Transactional
    public ApprovalQueue approve(UUID approvalId, String reviewerId, String comment) {
        ApprovalQueue approval = approvalRepo.findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException("Approval not found: " + approvalId));

        approval.setStatus(ApprovalStatus.APPROVED.name());
        approval.setReviewerId(reviewerId);
        approval.setComment(comment);
        approval.setReviewedAt(Instant.now());

        Page page = pageRepo.findById(approval.getPageId()).orElseThrow();
        page.setStatus(PageStatus.APPROVED);
        page.setApprovedAt(Instant.now());
        pageRepo.save(page);

        approvalRepo.save(approval);
        auditRepo.save(ApprovalAudit.builder()
                .approvalId(approvalId)
                .action("APPROVE")
                .reviewerId(reviewerId)
                .comment(comment)
                .build());

        log.info("Page {} approved by {}", page.getId(), reviewerId);
        return approval;
    }

    @Transactional
    public ApprovalQueue reject(UUID approvalId, String reviewerId, String comment) {
        ApprovalQueue approval = approvalRepo.findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException("Approval not found: " + approvalId));

        approval.setStatus(ApprovalStatus.REJECTED.name());
        approval.setReviewerId(reviewerId);
        approval.setComment(comment);
        approval.setReviewedAt(Instant.now());

        Page page = pageRepo.findById(approval.getPageId()).orElseThrow();
        page.setStatus(PageStatus.REJECTED);
        pageRepo.save(page);

        approvalRepo.save(approval);
        auditRepo.save(ApprovalAudit.builder()
                .approvalId(approvalId)
                .action("REJECT")
                .reviewerId(reviewerId)
                .comment(comment)
                .build());

        log.info("Page {} rejected by {}", page.getId(), reviewerId);
        return approval;
    }

    public List<ApprovalQueue> listPending() {
        return approvalRepo.findByStatus(ApprovalStatus.PENDING.name());
    }

    public List<ApprovalQueue> listByStatus(String status) {
        return approvalRepo.findByStatus(status);
    }

    /**
     * 获取页面的审批历史
     */
    public List<ApprovalQueue> getApprovalHistory(UUID pageId) {
        return approvalRepo.findByPageId(pageId);
    }

    /**
     * 获取审批审计追踪
     */
    public List<ApprovalAudit> getAuditTrail(UUID approvalId) {
        return auditRepo.findByApprovalId(approvalId);
    }
}
