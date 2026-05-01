package com.llmwiki.web.controller;

import com.llmwiki.common.enums.SyncStatus;
import com.llmwiki.domain.sync.entity.SyncLog;
import com.llmwiki.domain.sync.repository.SyncLogRepository;
import com.llmwiki.service.sync.SyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncControllerTest {

    @Mock SyncService syncService;
    @Mock SyncLogRepository syncLogRepo;
    @InjectMocks SyncController controller;

    @Test
    void shouldTriggerSync() {
        UUID sourceId = UUID.randomUUID();
        SyncLog expected = SyncLog.builder()
            .id(UUID.randomUUID())
            .sourceId(sourceId)
            .startedAt(Instant.now())
            .status(SyncStatus.SUCCESS)
            .fetchedCount(10)
            .processedCount(8)
            .skippedCount(2)
            .build();

        when(syncService.syncSource(sourceId)).thenReturn(expected);

        var response = controller.triggerSync(sourceId);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(10, response.getBody().getFetchedCount());
        assertEquals(8, response.getBody().getProcessedCount());
        verify(syncService).syncSource(sourceId);
    }
}
