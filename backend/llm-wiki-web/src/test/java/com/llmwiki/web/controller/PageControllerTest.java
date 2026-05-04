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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PageControllerTest {

    @Mock PageRepository pageRepo;
    @Mock PageLinkRepository pageLinkRepo;
    @Mock ProcessingLogRepository procLogRepo;

    @InjectMocks PageController controller;

    @Test
    void list_shouldReturnAllPagesWhenNoFilters() {
        Page page = Page.builder().id(UUID.randomUUID()).title("Test").build();
        when(pageRepo.findAll(any(PageRequest.class))).thenReturn(
                new org.springframework.data.domain.PageImpl<>(List.of(page)));

        var response = controller.list("ALL", "ALL", 0, 20);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals("Test", response.getBody().get(0).getTitle());
    }

    @Test
    void list_shouldFilterByStatusOnly() {
        Page page = Page.builder().id(UUID.randomUUID()).title("Filtered").build();
        when(pageRepo.findByStatus(eq("PENDING_APPROVAL"), any(PageRequest.class))).thenReturn(List.of(page));

        var response = controller.list("PENDING_APPROVAL", "ALL", 0, 20);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals("Filtered", response.getBody().get(0).getTitle());
    }

    @Test
    void list_shouldFilterByTypeOnly() {
        Page page = Page.builder().id(UUID.randomUUID()).title("Typed").pageType(PageType.ENTITY).build();
        when(pageRepo.findByPageType(eq(PageType.ENTITY), any(PageRequest.class))).thenReturn(List.of(page));

        var response = controller.list("ALL", "ENTITY", 0, 20);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void list_shouldFilterByStatusAndType() {
        Page page = Page.builder().id(UUID.randomUUID()).title("Both").build();
        when(pageRepo.findByStatusAndPageType(eq("PENDING_APPROVAL"), eq(PageType.ENTITY), any(PageRequest.class)))
                .thenReturn(List.of(page));

        var response = controller.list("PENDING_APPROVAL", "ENTITY", 0, 20);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void list_shouldRespectPagination() {
        when(pageRepo.findAll(any(PageRequest.class))).thenReturn(
                new org.springframework.data.domain.PageImpl<>(List.of()));

        controller.list("ALL", "ALL", 2, 10);

        verify(pageRepo).findAll(argThat((PageRequest pr) -> pr.getPageNumber() == 2 && pr.getPageSize() == 10));
    }

    @Test
    void getById_shouldReturnPageWhenFound() {
        UUID id = UUID.randomUUID();
        Page page = Page.builder().id(id).title("Found").build();
        when(pageRepo.findById(id)).thenReturn(Optional.of(page));

        var response = controller.getById(id);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Found", response.getBody().getTitle());
    }

    @Test
    void getById_shouldReturn404WhenNotFound() {
        UUID id = UUID.randomUUID();
        when(pageRepo.findById(id)).thenReturn(Optional.empty());

        var response = controller.getById(id);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void getLinks_shouldReturnLinksForPage() {
        UUID id = UUID.randomUUID();
        List<PageLink> links = List.of(PageLink.builder().sourcePageId(id).build());
        when(pageLinkRepo.findBySourcePageId(id)).thenReturn(links);

        var response = controller.getLinks(id);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getLinks_shouldReturnEmptyListWhenNoLinks() {
        UUID id = UUID.randomUUID();
        when(pageLinkRepo.findBySourcePageId(id)).thenReturn(List.of());

        var response = controller.getLinks(id);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void getHistory_shouldReturnProcessingLogs() {
        UUID id = UUID.randomUUID();
        List<ProcessingLog> logs = List.of(ProcessingLog.builder().rawDocumentId(id).build());
        when(procLogRepo.findByRawDocumentId(id)).thenReturn(logs);

        var response = controller.getHistory(id);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getHistory_shouldReturnEmptyListWhenNoLogs() {
        UUID id = UUID.randomUUID();
        when(procLogRepo.findByRawDocumentId(id)).thenReturn(List.of());

        var response = controller.getHistory(id);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void delete_shouldSoftDeletePage() {
        UUID id = UUID.randomUUID();
        Page page = Page.builder().id(id).title("To Delete").build();
        when(pageRepo.findById(id)).thenReturn(Optional.of(page));
        when(pageRepo.save(any(Page.class))).thenAnswer(i -> i.getArgument(0));

        var response = controller.delete(id);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(PageStatus.ARCHIVED, page.getStatus());
        verify(pageRepo).save(page);
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
