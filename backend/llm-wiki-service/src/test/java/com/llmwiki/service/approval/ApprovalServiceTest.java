package com.llmwiki.service.approval;

import com.llmwiki.common.enums.ApprovalAction;
import com.llmwiki.common.enums.ApprovalStatus;
import com.llmwiki.common.enums.PageStatus;
import com.llmwiki.domain.approval.entity.ApprovalQueue;
import com.llmwiki.domain.approval.repository.ApprovalAuditRepository;
import com.llmwiki.domain.approval.repository.ApprovalQueueRepository;
import com.llmwiki.domain.page.entity.Page;
import com.llmwiki.domain.page.repository.PageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    @Mock ApprovalQueueRepository approvalRepo;
    @Mock ApprovalAuditRepository auditRepo;
    @Mock PageRepository pageRepo;

    @InjectMocks
    ApprovalService approvalService;

    UUID pageId;
    Page page;

    @BeforeEach
    void setUp() {
        pageId = UUID.randomUUID();
        page = Page.builder()
                .id(pageId)
                .title("Test Page")
                .slug("test-page")
                .pageType(com.llmwiki.common.enums.PageType.ENTITY)
                .status(PageStatus.PENDING_APPROVAL)
                .aiScore(new BigDecimal("8.0"))
                .build();
    }

    @Test
    void submitForApproval_shouldCreateApprovalAndSetPageStatus() {
        when(pageRepo.findById(pageId)).thenReturn(Optional.of(page));
        when(approvalRepo.save(any(ApprovalQueue.class))).thenAnswer(i -> i.getArgument(0));

        ApprovalQueue result = approvalService.submitForApproval(pageId, ApprovalAction.CREATE, "New page");

        assertEquals(pageId, result.getPageId());
        assertEquals("CREATE", result.getAction());
        assertEquals("New page", result.getComment());
        assertEquals(ApprovalStatus.PENDING.name(), result.getStatus());

        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(pageRepo).save(pageCaptor.capture());
        assertEquals(PageStatus.PENDING_APPROVAL, pageCaptor.getValue().getStatus());
    }

    @Test
    void submitForApproval_shouldThrowWhenPageNotFound() {
        when(pageRepo.findById(pageId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> approvalService.submitForApproval(pageId, ApprovalAction.CREATE, ""));
    }

    @Test
    void approve_shouldUpdateApprovalAndPageStatus() {
        UUID approvalId = UUID.randomUUID();
        ApprovalQueue approval = ApprovalQueue.builder()
                .id(approvalId).pageId(pageId).action("CREATE")
                .status(ApprovalStatus.PENDING.name()).build();

        when(approvalRepo.findById(approvalId)).thenReturn(Optional.of(approval));
        when(pageRepo.findById(pageId)).thenReturn(Optional.of(page));
        when(approvalRepo.save(any(ApprovalQueue.class))).thenAnswer(i -> i.getArgument(0));

        ApprovalQueue result = approvalService.approve(approvalId, "admin", "Looks good");

        assertEquals(ApprovalStatus.APPROVED.name(), result.getStatus());
        assertEquals("admin", result.getReviewerId());
        assertEquals("Looks good", result.getComment());
        assertNotNull(result.getReviewedAt());

        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(pageRepo).save(pageCaptor.capture());
        assertEquals(PageStatus.APPROVED, pageCaptor.getValue().getStatus());
        assertNotNull(pageCaptor.getValue().getApprovedAt());
    }

    @Test
    void reject_shouldUpdateApprovalAndPageStatus() {
        UUID approvalId = UUID.randomUUID();
        ApprovalQueue approval = ApprovalQueue.builder()
                .id(approvalId).pageId(pageId).action("CREATE")
                .status(ApprovalStatus.PENDING.name()).build();

        when(approvalRepo.findById(approvalId)).thenReturn(Optional.of(approval));
        when(pageRepo.findById(pageId)).thenReturn(Optional.of(page));
        when(approvalRepo.save(any(ApprovalQueue.class))).thenAnswer(i -> i.getArgument(0));

        ApprovalQueue result = approvalService.reject(approvalId, "admin", "Needs work");

        assertEquals(ApprovalStatus.REJECTED.name(), result.getStatus());
        assertEquals("admin", result.getReviewerId());

        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(pageRepo).save(pageCaptor.capture());
        assertEquals(PageStatus.REJECTED, pageCaptor.getValue().getStatus());
    }

    @Test
    void approve_shouldThrowWhenApprovalNotFound() {
        UUID approvalId = UUID.randomUUID();
        when(approvalRepo.findById(approvalId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> approvalService.approve(approvalId, "admin", ""));
    }

    @Test
    void listPending_shouldReturnOnlyPending() {
        ApprovalQueue pending1 = ApprovalQueue.builder().id(UUID.randomUUID()).status(ApprovalStatus.PENDING.name()).build();
        ApprovalQueue pending2 = ApprovalQueue.builder().id(UUID.randomUUID()).status(ApprovalStatus.PENDING.name()).build();
        when(approvalRepo.findByStatus(ApprovalStatus.PENDING.name())).thenReturn(List.of(pending1, pending2));

        List<ApprovalQueue> result = approvalService.listPending();

        assertEquals(2, result.size());
    }
}
