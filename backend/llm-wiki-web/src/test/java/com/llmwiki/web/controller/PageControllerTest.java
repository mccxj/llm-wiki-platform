package com.llmwiki.web.controller;

import com.llmwiki.common.enums.PageStatus;
import com.llmwiki.common.enums.PageType;
import com.llmwiki.domain.page.entity.Page;
import com.llmwiki.domain.page.entity.PageLink;
import com.llmwiki.domain.page.repository.PageLinkRepository;
import com.llmwiki.domain.page.repository.PageRepository;
import com.llmwiki.domain.processing.entity.ProcessingLog;
import com.llmwiki.domain.processing.repository.ProcessingLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PageControllerTest {

    @Mock PageRepository pageRepo;
    @Mock PageLinkRepository pageLinkRepo;
    @Mock ProcessingLogRepository procLogRepo;

    @InjectMocks
    PageController controller;

    @Test
    void list_shouldReturnAllPagesWhenNoFilter() {
        Page page = Page.builder()
                .id(UUID.randomUUID()).title("Test").slug("test")
                .status(PageStatus.APPROVED).pageType(PageType.ENTITY)
                .aiScore(new BigDecimal("8.0")).build();
        when(pageRepo.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(page)));

        var response = controller.list("ALL", "ALL", 0, 20);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void list_shouldFilterByStatus() {
        Page page = Page.builder()
                .id(UUID.randomUUID()).title("Pending").slug("pending")
                .status(PageStatus.PENDING_APPROVAL).pageType(PageType.ENTITY).build();
        when(pageRepo.findByStatus(eq("PENDING_APPROVAL"), any()))
                .thenReturn(List.of(page));

        var response = controller.list("PENDING_APPROVAL", "ALL", 0, 20);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals("Pending", response.getBody().get(0).getTitle());
    }

    @Test
    void list_shouldFilterByType() {
        Page page = Page.builder()
                .id(UUID.randomUUID()).title("Entity").slug("entity")
                .status(PageStatus.APPROVED).pageType(PageType.ENTITY).build();
        when(pageRepo.findByPageType(eq(PageType.ENTITY), any()))
                .thenReturn(List.of(page));

        var response = controller.list("ALL", "ENTITY", 0, 20);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void list_shouldFilterByStatusAndType() {
        Page page = Page.builder()
                .id(UUID.randomUUID()).title("Filtered").slug("filtered")
                .status(PageStatus.APPROVED).pageType(PageType.ENTITY).build();
        when(pageRepo.findByStatusAndPageType(eq("APPROVED"), eq(PageType.ENTITY), any()))
                .thenReturn(List.of(page));

        var response = controller.list("APPROVED", "ENTITY", 0, 20);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getById_shouldReturnPageWhenFound() {
        UUID id = UUID.randomUUID();
        Page page = Page.builder()
                .id(id).title("Test").slug("test")
                .status(PageStatus.APPROVED).build();
        when(pageRepo.findById(id)).thenReturn(Optional.of(page));

        var response = controller.getById(id);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Test", response.getBody().getTitle());
    }

    @Test
    void getById_shouldReturn404WhenNotFound() {
        UUID id = UUID.randomUUID();
        when(pageRepo.findById(id)).thenReturn(Optional.empty());

        var response = controller.getById(id);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void getLinks_shouldReturnPageLinks() {
        UUID pageId = UUID.randomUUID();
        PageLink link = PageLink.builder()
                .id(UUID.randomUUID()).sourcePageId(pageId)
                .targetPageId(UUID.randomUUID())
                .linkType(com.llmwiki.common.enums.EdgeType.RELATED_TO).build();
        when(pageLinkRepo.findBySourcePageId(pageId)).thenReturn(List.of(link));

        var response = controller.getLinks(pageId);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getLinks_shouldReturnEmptyListWhenNoLinks() {
        UUID pageId = UUID.randomUUID();
        when(pageLinkRepo.findBySourcePageId(pageId)).thenReturn(List.of());

        var response = controller.getLinks(pageId);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void getHistory_shouldReturnProcessingLogs() {
        UUID pageId = UUID.randomUUID();
        ProcessingLog log = ProcessingLog.builder()
                .id(UUID.randomUUID()).rawDocumentId(pageId)
                .step("SCORE").status(com.llmwiki.common.enums.StepStatus.SUCCESS)
                .detail("Score: 8.0").build();
        when(procLogRepo.findByRawDocumentId(pageId)).thenReturn(List.of(log));

        var response = controller.getHistory(pageId);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals("SCORE", response.getBody().get(0).getStep());
    }

    @Test
    void getHistory_shouldReturnEmptyListWhenNoLogs() {
        UUID pageId = UUID.randomUUID();
        when(procLogRepo.findByRawDocumentId(pageId)).thenReturn(List.of());

        var response = controller.getHistory(pageId);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void delete_shouldArchivePage() {
        UUID id = UUID.randomUUID();
        Page page = Page.builder()
                .id(id).title("To Delete").slug("to-delete")
                .status(PageStatus.APPROVED).build();
        when(pageRepo.findById(id)).thenReturn(Optional.of(page));
        when(pageRepo.save(any(Page.class))).thenAnswer(i -> i.getArgument(0));

        var response = controller.delete(id);

        assertEquals(200, response.getStatusCodeValue());
        ArgumentCaptor<Page> captor = ArgumentCaptor.forClass(Page.class);
        verify(pageRepo).save(captor.capture());
        assertEquals(PageStatus.ARCHIVED, captor.getValue().getStatus());
    }

    @Test
    void delete_shouldReturn404WhenNotFound() {
        UUID id = UUID.randomUUID();
        when(pageRepo.findById(id)).thenReturn(Optional.empty());

        var response = controller.delete(id);

        assertEquals(404, response.getStatusCodeValue());
        verify(pageRepo, never()).save(any());
    }
}
