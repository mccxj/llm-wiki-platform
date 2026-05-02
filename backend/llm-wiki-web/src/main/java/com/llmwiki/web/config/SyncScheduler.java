package com.llmwiki.web.config;

import com.llmwiki.domain.sync.repository.WikiSourceRepository;
import com.llmwiki.service.sync.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.UUID;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "sync.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class SyncScheduler {

    private final SyncService syncService;
    private final WikiSourceRepository sourceRepo;

    @Scheduled(cron = "${sync.default-cron:0 */6 * * * *}")
    public void scheduledSync() {
        log.info("Starting scheduled sync");
        sourceRepo.findByEnabledTrue().forEach(source -> {
            try {
                syncService.syncSource(source.getId());
            } catch (Exception e) {
                log.error("Scheduled sync failed for source: {}", source.getId(), e);
            }
        });
    }
}
