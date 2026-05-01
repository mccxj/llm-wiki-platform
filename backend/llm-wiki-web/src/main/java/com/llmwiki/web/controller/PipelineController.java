package com.llmwiki.web.controller;

import com.llmwiki.domain.page.entity.Page;
import com.llmwiki.domain.page.repository.PageRepository;
import com.llmwiki.domain.pipeline.entity.DeadLetterQueue;
import com.llmwiki.domain.pipeline.repository.DeadLetterQueueRepository;
import com.llmwiki.domain.processing.entity.ProcessingLog;
import com.llmwiki.domain.processing.repository.ProcessingLogRepository;
import com.llmwiki.domain.sync.entity.RawDocument;
import com.llmwiki.domain.sync.repository.RawDocumentRepository;
import com.llmwiki.service.pipeline.PipelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pipeline")
@RequiredArgsConstructor
public class PipelineController {

    private final PipelineService pipelineService;
    private final RawDocumentRepository rawDocRepo;
    private final ProcessingLogRepository procLogRepo;
    private final PageRepository pageRepo;
    private final DeadLetterQueueRepository deadLetterQueueRepo;

    /**
     * Trigger pipeline processing for a raw document.
     */
    @PostMapping("/process/{rawDocId}")
    public ResponseEntity<?> process(@PathVariable UUID rawDocId) {
        try {
            pipelineService.processDocument(rawDocId);
            return ResponseEntity.ok("Pipeline completed for document: " + rawDocId);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Pipeline failed: " + e.getMessage());
        }
    }

    /**
     * Get processing logs for a document.
     */
    @GetMapping("/logs/{rawDocId}")
    public ResponseEntity<List<ProcessingLog>> getLogs(@PathVariable UUID rawDocId) {
        return ResponseEntity.ok(procLogRepo.findByRawDocumentId(rawDocId));
    }

    /**
     * List all raw documents pending processing.
     */
    @GetMapping("/pending")
    public ResponseEntity<List<RawDocument>> getPending() {
        return ResponseEntity.ok(rawDocRepo.findAll());
    }

    /**
     * List all generated pages.
     */
    @GetMapping("/pages")
    public ResponseEntity<List<Page>> getPages(@RequestParam(defaultValue = "ALL") String status) {
        if ("ALL".equals(status)) {
            return ResponseEntity.ok(pageRepo.findAll());
        }
        return ResponseEntity.ok(pageRepo.findByStatus(status));
    }

    @GetMapping("/pages/{id}")
    public ResponseEntity<Page> getPage(@PathVariable UUID id) {
        return pageRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * List all dead letter queue entries, optionally filtered by status.
     */
    @GetMapping("/dead-letters")
    public ResponseEntity<List<DeadLetterQueue>> getDeadLetters(
            @RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(deadLetterQueueRepo.findByStatus(status));
        }
        return ResponseEntity.ok(deadLetterQueueRepo.findAll());
    }

    /**
     * Retry a specific dead letter entry.
     */
    @PostMapping("/retry/{deadLetterId}")
    public ResponseEntity<?> retryDeadLetter(@PathVariable UUID deadLetterId) {
        try {
            pipelineService.retryDeadLetter(deadLetterId);
            return ResponseEntity.ok("Dead letter " + deadLetterId + " retried successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Retry failed: " + e.getMessage());
        }
    }
}
