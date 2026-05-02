package com.llmwiki.web.config;

import com.llmwiki.domain.maintenance.entity.MaintenanceReportLog;
import com.llmwiki.domain.maintenance.repository.MaintenanceReportLogRepository;
import com.llmwiki.domain.page.entity.Page;
import com.llmwiki.service.maintenance.MaintenanceService;
import com.llmwiki.service.maintenance.DuplicateGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceSchedulerTest {

    @Mock MaintenanceService maintenanceService;
    @Mock MaintenanceReportLogRepository reportLogRepo;

    @InjectMocks
    MaintenanceScheduler scheduler;

    @BeforeEach
    void setUp() {
        when(reportLogRepo.save(any(MaintenanceReportLog.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void detectOrphansShouldRunAndLogResult() {
        // Given
        Page orphan = Page.builder().id(UUID.randomUUID()).title("Orphan").slug("orphan").build();
        when(maintenanceService.findOrphans()).thenReturn(List.of(orphan));

        // When
        scheduler.detectOrphans();

        // Then
        verify(maintenanceService).findOrphans();
        ArgumentCaptor<MaintenanceReportLog> captor = ArgumentCaptor.forClass(MaintenanceReportLog.class);
        verify(reportLogRepo).save(captor.capture());
        MaintenanceReportLog log = captor.getValue();
        assertEquals("DETECT_ORPHANS", log.getTaskType());
        assertEquals("COMPLETED", log.getStatus());
        assertNotNull(log.getResult());
    }

    @Test
    void detectStaleShouldRunAndLogResult() {
        // Given
        Page stale = Page.builder().id(UUID.randomUUID()).title("Stale").slug("stale").build();
        Page contradiction = Page.builder().id(UUID.randomUUID()).title("Contested").slug("contested").build();
        when(maintenanceService.findStalePages(30)).thenReturn(List.of(stale));
        when(maintenanceService.findContradictions()).thenReturn(List.of(contradiction));

        // When
        scheduler.detectStale();

        // Then
        verify(maintenanceService).findStalePages(30);
        verify(maintenanceService).findContradictions();
        ArgumentCaptor<MaintenanceReportLog> captor = ArgumentCaptor.forClass(MaintenanceReportLog.class);
        verify(reportLogRepo, atLeast(2)).save(captor.capture());
        List<MaintenanceReportLog> logs = captor.getAllValues();
        assertTrue(logs.stream().anyMatch(l -> "DETECT_STALE".equals(l.getTaskType())));
        assertTrue(logs.stream().anyMatch(l -> "DETECT_CONTRADICTIONS".equals(l.getTaskType())));
    }

    @Test
    void detectDuplicatesShouldRunMonthlyAndLogResult() {
        // Given
        Page dup1 = Page.builder().id(UUID.randomUUID()).title("Dup").slug("dup-1").build();
        Page dup2 = Page.builder().id(UUID.randomUUID()).title("Dup").slug("dup-2").build();
        Page splitPage = Page.builder().id(UUID.randomUUID()).title("Long Page").slug("long").build();

        when(maintenanceService.findDuplicates()).thenReturn(List.of(new DuplicateGroup(List.of(dup1, dup2), 1.0)));
        when(maintenanceService.findSplitSuggestions()).thenReturn(List.of(splitPage));

        // When
        scheduler.detectDuplicates();

        // Then
        verify(maintenanceService).findDuplicates();
        verify(maintenanceService).findSplitSuggestions();
        ArgumentCaptor<MaintenanceReportLog> captor = ArgumentCaptor.forClass(MaintenanceReportLog.class);
        verify(reportLogRepo, atLeast(2)).save(captor.capture());
        List<MaintenanceReportLog> logs = captor.getAllValues();
        assertTrue(logs.stream().anyMatch(l -> "FIND_DUPLICATES".equals(l.getTaskType())));
        assertTrue(logs.stream().anyMatch(l -> "FIND_SPLIT_SUGGESTIONS".equals(l.getTaskType())));
    }

    @Test
    void detectOrphansShouldHandleExceptionAndLogError() {
        // Given
        when(maintenanceService.findOrphans()).thenThrow(new RuntimeException("DB error"));

        // When - should not throw
        scheduler.detectOrphans();

        // Then
        ArgumentCaptor<MaintenanceReportLog> captor = ArgumentCaptor.forClass(MaintenanceReportLog.class);
        verify(reportLogRepo).save(captor.capture());
        MaintenanceReportLog log = captor.getValue();
        assertEquals("DETECT_ORPHANS", log.getTaskType());
        assertEquals("FAILED", log.getStatus());
        assertTrue(log.getResult().contains("DB error"));
    }
}
