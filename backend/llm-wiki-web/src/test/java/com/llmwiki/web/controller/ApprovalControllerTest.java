package com.llmwiki.web.controller;

import com.llmwiki.common.enums.ApprovalAction;
import com.llmwiki.common.enums.ApprovalStatus;
import com.llmwiki.domain.approval.entity.ApprovalQueue;
import com.llmwiki.domain.approval.repository.ApprovalQueueRepository;
import com.llmwiki.service.approval.ApprovalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalControllerTest {

    @Mock ApprovalService approvalService;
    @Mock ApprovalQueueRepository approvalRepo;
    @InjectMocks ApprovalController controller;

    @Test
    void submit_shouldDelegateToService() {
        UUID pageId = UUID.randomUUID();
        ApprovalQueue expected = ApprovalQueue.builder()
                .id(UUID.randomUUID()).pageId(pageId)
                .action("CREATE").status(ApprovalStatus.PENDING.name()).build();
        when(approvalService.submitForApproval(eq(pageId), eq(ApprovalAction.CREATE), eq("new page")))
                .thenReturn(expected);

        var response = controller.submit(pageId, Map.of("action", "CREATE", "comment", "new page"));

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(expected, response.getBody());
    }

    @Test
    void approve_shouldDelegateToService() {
        UUID approvalId = UUID.randomUUID();
        ApprovalQueue expected = ApprovalQueue.builder()
                .id(approvalId).status(ApprovalStatus.APPROVED.name()).build();
        when(approvalService.approve(approvalId, "admin", "ok")).thenReturn(expected);

        var response = controller.approve(approvalId, Map.of("reviewerId", "admin", "comment", "ok"));

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(ApprovalStatus.APPROVED.name(), response.getBody().getStatus());
    }

    @Test
    void reject_shouldDelegateToService() {
        UUID approvalId = UUID.randomUUID();
        ApprovalQueue expected = ApprovalQueue.builder()
                .id(approvalId).status(ApprovalStatus.REJECTED.name()).build();
        when(approvalService.reject(approvalId, "admin", "bad")).thenReturn(expected);

        var response = controller.reject(approvalId, Map.of("reviewerId", "admin", "comment", "bad"));

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(ApprovalStatus.REJECTED.name(), response.getBody().getStatus());
    }

    @Test
    void pending_shouldReturnPendingList() {
        List<ApprovalQueue> expected = List.of(
                ApprovalQueue.builder().id(UUID.randomUUID()).build());
        when(approvalService.listPending()).thenReturn(expected);

        var response = controller.pending();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void list_shouldReturnByStatus() {
        List<ApprovalQueue> expected = List.of(
                ApprovalQueue.builder().id(UUID.randomUUID()).build());
        when(approvalService.listByStatus("PENDING")).thenReturn(expected);

        var response = controller.list("PENDING");

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getById_shouldReturnApproval() {
        UUID id = UUID.randomUUID();
        ApprovalQueue expected = ApprovalQueue.builder().id(id).build();
        when(approvalRepo.findById(id)).thenReturn(java.util.Optional.of(expected));

        var response = controller.getById(id);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(id, response.getBody().getId());
    }

    @Test
    void getById_shouldReturn404WhenNotFound() {
        UUID id = UUID.randomUUID();
        when(approvalRepo.findById(id)).thenReturn(java.util.Optional.empty());

        var response = controller.getById(id);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void batch_shouldApproveMultiple() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(approvalService.approve(eq(id1), eq("admin"), eq("batch approve")))
                .thenReturn(ApprovalQueue.builder().id(id1).build());
        when(approvalService.approve(eq(id2), eq("admin"), eq("batch approve")))
                .thenReturn(ApprovalQueue.builder().id(id2).build());

        var response = controller.batch(Map.of(
                "ids", List.of(id1.toString(), id2.toString()),
                "action", "approve",
                "comment", "batch approve",
                "reviewerId", "admin"));

        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertEquals(2, body.get("success"));
        assertEquals(0, body.get("failed"));
    }

    @Test
    void batch_shouldRejectMultiple() {
        UUID id1 = UUID.randomUUID();
        when(approvalService.reject(eq(id1), eq("admin"), eq("batch reject")))
                .thenReturn(ApprovalQueue.builder().id(id1).build());

        var response = controller.batch(Map.of(
                "ids", List.of(id1.toString()),
                "action", "reject",
                "comment", "batch reject"));

        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertEquals(1, body.get("success"));
    }

    @Test
    void batch_shouldHandlePartialFailures() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(approvalService.approve(eq(id1), eq("admin"), eq("")))
                .thenReturn(ApprovalQueue.builder().id(id1).build());
        when(approvalService.approve(eq(id2), eq("admin"), eq("")))
                .thenThrow(new RuntimeException("Not found"));

        var response = controller.batch(Map.of(
                "ids", List.of(id1.toString(), id2.toString()),
                "action", "approve"));

        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertEquals(1, body.get("success"));
        assertEquals(1, body.get("failed"));
    }

    @Test
    void history_shouldReturnApprovalHistory() {
        UUID pageId = UUID.randomUUID();
        List<ApprovalQueue> history = List.of(
                ApprovalQueue.builder().id(UUID.randomUUID()).pageId(pageId).build());
        when(approvalService.getApprovalHistory(pageId)).thenReturn(history);

        var response = controller.history(pageId, 0, 20);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }
}
