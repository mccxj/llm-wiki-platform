package com.llmwiki.web.controller;

import com.llmwiki.domain.config.entity.SystemConfig;
import com.llmwiki.domain.config.repository.SystemConfigRepository;
import com.llmwiki.domain.page.entity.Page;
import com.llmwiki.service.maintenance.MaintenanceReport;
import com.llmwiki.service.maintenance.MaintenanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private SystemConfigRepository configRepo;

    @Mock
    private MaintenanceService maintenanceService;

    @InjectMocks
    private AdminController adminController;

    @Test
    void getConfig_shouldReturnAllConfigs() {
        List<SystemConfig> configs = List.of(new SystemConfig(), new SystemConfig());
        when(configRepo.findAll()).thenReturn(configs);

        var response = adminController.getConfig();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void updateConfig_shouldCreateNewConfigWhenNotFound() {
        when(configRepo.findById("test.key")).thenReturn(java.util.Optional.empty());
        when(configRepo.save(any(SystemConfig.class))).thenAnswer(i -> i.getArgument(0));

        var response = adminController.updateConfig("test.key",
                Map.of("value", "test-value", "description", "desc"));

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("test.key", response.getBody().getKey());
        assertEquals("test-value", response.getBody().getValue());
    }

    @Test
    void updateConfig_shouldReturnBadRequestWhenValueMissing() {
        var response = adminController.updateConfig("test.key", Map.of());

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void updateConfig_shouldUpdateExistingConfig() {
        SystemConfig existing = new SystemConfig();
        existing.setKey("test.key");
        existing.setValue("old-value");
        when(configRepo.findById("test.key")).thenReturn(java.util.Optional.of(existing));
        when(configRepo.save(any(SystemConfig.class))).thenAnswer(i -> i.getArgument(0));

        var response = adminController.updateConfig("test.key",
                Map.of("value", "new-value"));

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("new-value", response.getBody().getValue());
    }

    @Test
    void triggerOrphanCheck_shouldReturnOrphans() {
        List<Page> orphans = List.of(Page.builder().title("Orphan1").build());
        when(maintenanceService.findOrphans()).thenReturn(orphans);

        var response = adminController.triggerOrphanCheck();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void triggerStaleCheck_shouldReturnStalePages() {
        List<Page> stale = List.of(Page.builder().title("Stale1").build());
        when(maintenanceService.findStalePages(30)).thenReturn(stale);

        var response = adminController.triggerStaleCheck(30);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void triggerStaleCheck_shouldUseDefaultDays() {
        when(maintenanceService.findStalePages(30)).thenReturn(List.of());

        adminController.triggerStaleCheck(30);

        verify(maintenanceService).findStalePages(30);
    }

    @Test
    void triggerDuplicateCheck_shouldReturnDuplicates() {
        var dupGroup = new com.llmwiki.service.maintenance.DuplicateGroup(
                List.of(Page.builder().title("Dup").build()), 0.9);
        when(maintenanceService.findDuplicates()).thenReturn(List.of(dupGroup));

        var response = adminController.triggerDuplicateCheck();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void triggerContradictionCheck_shouldReturnContradictions() {
        List<Page> contradictions = List.of(Page.builder().title("Contested").build());
        when(maintenanceService.findContradictions()).thenReturn(contradictions);

        var response = adminController.triggerContradictionCheck();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getMaintenanceReport_shouldReturnReport() {
        MaintenanceReport report = new MaintenanceReport();
        report.setTotalPages(50L);
        when(maintenanceService.generateReport()).thenReturn(report);

        var response = adminController.getMaintenanceReport();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(50L, response.getBody().getTotalPages());
    }
}
