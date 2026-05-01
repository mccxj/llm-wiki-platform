package com.llmwiki.service.sync;

import com.llmwiki.adapter.api.WikiSourceAdapter;
import com.llmwiki.adapter.dto.RawDocumentDTO;
import com.llmwiki.common.enums.SyncStatus;
import com.llmwiki.domain.sync.entity.RawDocument;
import com.llmwiki.domain.sync.entity.SyncLog;
import com.llmwiki.domain.sync.entity.WikiSource;
import com.llmwiki.domain.sync.repository.RawDocumentRepository;
import com.llmwiki.domain.sync.repository.SyncLogRepository;
import com.llmwiki.domain.sync.repository.WikiSourceRepository;
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
public class SyncService {

    private final WikiSourceRepository sourceRepo;
    private final RawDocumentRepository rawDocRepo;
    private final SyncLogRepository syncLogRepo;
    private final WikiSourceAdapterFactory adapterFactory;

    @Transactional
    public SyncLog syncSource(UUID sourceId) {
        WikiSource source = sourceRepo.findById(sourceId)
            .orElseThrow(() -> new IllegalArgumentException("Source not found: " + sourceId));

        WikiSourceAdapter adapter = adapterFactory.getAdapter(source.getAdapterClass());
        SyncLog syncLog = SyncLog.builder()
            .sourceId(sourceId)
            .startedAt(Instant.now())
            .status(SyncStatus.RUNNING)
            .build();

        try {
            List<RawDocumentDTO> changes = adapter.fetchChanges(
                source.getLastSyncAt() != null ? source.getLastSyncAt() : Instant.EPOCH);

            int processed = 0, skipped = 0, failed = 0;

            for (RawDocumentDTO doc : changes) {
                try {
                    String hash = computeHash(doc.getContent());
                    var existing = rawDocRepo.findBySourceId(doc.getSourceId());

                    if (existing.isPresent() && existing.get().getContentHash().equals(hash)) {
                        skipped++;
                        continue;
                    }

                    RawDocument rawDoc = saveRawDocument(doc, hash, existing.orElse(null));
                    processed++;
                } catch (Exception e) {
                    log.error("Failed to process document: {}", doc.getSourceId(), e);
                    failed++;
                }
            }

            syncLog.setFetchedCount(changes.size());
            syncLog.setProcessedCount(processed);
            syncLog.setSkippedCount(skipped);
            syncLog.setFailedCount(failed);
            syncLog.setStatus(failed > 0 ? SyncStatus.PARTIAL : SyncStatus.SUCCESS);
            syncLog.setFinishedAt(Instant.now());

            source.setLastSyncAt(Instant.now());
            sourceRepo.save(source);

        } catch (Exception e) {
            log.error("Sync failed for source: {}", sourceId, e);
            syncLog.setStatus(SyncStatus.FAILED);
            syncLog.setErrorMessage(e.getMessage());
            syncLog.setFinishedAt(Instant.now());
        }

        return syncLogRepo.save(syncLog);
    }

    private RawDocument saveRawDocument(RawDocumentDTO doc, String hash, RawDocument existing) {
        RawDocument.RawDocumentBuilder builder = RawDocument.builder()
            .sourceId(doc.getSourceId())
            .title(doc.getTitle())
            .content(doc.getContent())
            .contentHash(hash)
            .sourceUrl(doc.getSourceUrl())
            .lastCheckedAt(Instant.now());

        if (existing != null) {
            builder.id(existing.getId());
            builder.sourceName(existing.getSourceName());
            builder.ingestedAt(existing.getIngestedAt());
        }

        return rawDocRepo.save(builder.build());
    }

    private String computeHash(String content) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute hash", e);
        }
    }
}
