package com.llmwiki.web.controller;

import com.llmwiki.domain.sync.entity.SyncLog;
import com.llmwiki.domain.sync.entity.WikiSource;
import com.llmwiki.domain.sync.repository.SyncLogRepository;
import com.llmwiki.domain.sync.repository.WikiSourceRepository;
import com.llmwiki.service.sync.SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;
    private final WikiSourceRepository sourceRepo;
    private final SyncLogRepository syncLogRepo;

    @PostMapping("/trigger/{sourceId}")
    public ResponseEntity<SyncLog> triggerSync(@PathVariable UUID sourceId) {
        return ResponseEntity.ok(syncService.syncSource(sourceId));
    }

    @PostMapping("/trigger-all")
    public ResponseEntity<List<SyncLog>> triggerAll() {
        List<SyncLog> results = sourceRepo.findAll().stream()
                .map(source -> {
                    try {
                        return syncService.syncSource(source.getId());
                    } catch (Exception e) {
                        SyncLog errorLog = SyncLog.builder()
                                .sourceId(source.getId())
                                .status(com.llmwiki.common.enums.SyncStatus.FAILED)
                                .errorMessage(e.getMessage())
                                .build();
                        return syncLogRepo.save(errorLog);
                    }
                })
                .toList();
        return ResponseEntity.ok(results);
    }

    @GetMapping("/logs")
    public ResponseEntity<List<SyncLog>> getLogs() {
        return ResponseEntity.ok(syncLogRepo.findAll());
    }

    @GetMapping("/sources")
    public ResponseEntity<List<WikiSource>> getSources() {
        return ResponseEntity.ok(sourceRepo.findAll());
    }
}
