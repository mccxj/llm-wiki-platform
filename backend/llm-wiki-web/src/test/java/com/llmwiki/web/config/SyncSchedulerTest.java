package com.llmwiki.web.config;

import com.llmwiki.domain.sync.entity.WikiSource;
import com.llmwiki.domain.sync.repository.WikiSourceRepository;
import com.llmwiki.service.sync.SyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncSchedulerTest {

    @Mock SyncService syncService;
    @Mock WikiSourceRepository sourceRepo;

    @InjectMocks
    SyncScheduler syncScheduler;

    @Test
    void scheduledSync_shouldOnlySyncEnabledSources() {
        WikiSource enabled = WikiSource.builder()
                .id(UUID.randomUUID()).name("Enabled Source")
                .adapterClass("test").enabled(true).build();
        WikiSource disabled = WikiSource.builder()
                .id(UUID.randomUUID()).name("Disabled Source")
                .adapterClass("test").enabled(false).build();

        when(sourceRepo.findByEnabledTrue()).thenReturn(List.of(enabled));

        syncScheduler.scheduledSync();

        verify(syncService).syncSource(enabled.getId());
        verify(syncService, never()).syncSource(disabled.getId());
    }

    @Test
    void scheduledSync_shouldHandleEmptySources() {
        when(sourceRepo.findByEnabledTrue()).thenReturn(List.of());

        syncScheduler.scheduledSync();

        verifyNoInteractions(syncService);
    }
}
