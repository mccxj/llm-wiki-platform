package com.llmwiki.web.controller;

import com.llmwiki.domain.page.entity.Page;
import com.llmwiki.domain.page.repository.PageRepository;
import com.llmwiki.domain.processing.entity.ProcessingLog;
import com.llmwiki.domain.processing.repository.ProcessingLogRepository;
import com.llmwiki.domain.sync.entity.RawDocument;
import com.llmwiki.domain.sync.repository.RawDocumentRepository;
import com.llmwiki.service.pipeline.PipelineService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineControllerTest {

    @Mock PipelineService pipelineService;
    @Mock RawDocumentRepository rawDocRepo;
    @Mock ProcessingLogRepository procLogRepo;
    @Mock PageRepository pageRepo;
    @InjectMocks PipelineController controller;

    @Test
    void process_shouldReturnOkOnSuccess() {
        UUID rawDocId = UUID.randomUUID();
        doNothing().when(pipelineService).processDocument(rawDocId);

        var response = controller.process(rawDocId);

        assertEquals(200, response.getStatusCodeValue());
        verify(pipelineService).processDocument(rawDocId);
    }

    @Test
    void process_shouldReturn500OnError() {
        UUID rawDocId = UUID.randomUUID();
        doThrow(new RuntimeException("fail")).when(pipelineService).processDocument(rawDocId);

        var response = controller.process(rawDocId);

        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Pipeline failed"));
    }

    @Test
    void getLogs_shouldReturnLogs() {
        UUID rawDocId = UUID.randomUUID();
        List<ProcessingLog> logs = List.of(ProcessingLog.builder().build());
        when(procLogRepo.findByRawDocumentId(rawDocId)).thenReturn(logs);

        var response = controller.getLogs(rawDocId);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getPending_shouldReturnAllRawDocs() {
        List<RawDocument> docs = List.of(RawDocument.builder().build());
        when(rawDocRepo.findAll()).thenReturn(docs);

        var response = controller.getPending();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getPages_shouldReturnAllWhenStatusAll() {
        List<Page> pages = List.of(Page.builder().build());
        when(pageRepo.findAll()).thenReturn(pages);

        var response = controller.getPages("ALL");

        assertEquals(200, response.getStatusCodeValue());
        verify(pageRepo).findAll();
    }

    @Test
    void getPages_shouldFilterByStatus() {
        List<Page> pages = List.of(Page.builder().build());
        when(pageRepo.findByStatus("APPROVED")).thenReturn(pages);

        var response = controller.getPages("APPROVED");

        assertEquals(200, response.getStatusCodeValue());
        verify(pageRepo).findByStatus("APPROVED");
    }

    @Test
    void getPage_shouldReturn404WhenNotFound() {
        UUID id = UUID.randomUUID();
        when(pageRepo.findById(id)).thenReturn(java.util.Optional.empty());

        var response = controller.getPage(id);

        assertEquals(404, response.getStatusCodeValue());
    }
}
