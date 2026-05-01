package com.llmwiki.service.maintenance;

import com.llmwiki.domain.graph.repository.KgEdgeRepository;
import com.llmwiki.domain.graph.repository.KgNodeRepository;
import com.llmwiki.domain.page.entity.Page;
import com.llmwiki.domain.page.repository.PageRepository;
import com.llmwiki.domain.processing.repository.ProcessingLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceServiceTest {

    @Mock KgNodeRepository kgNodeRepo;
    @Mock KgEdgeRepository kgEdgeRepo;
    @Mock PageRepository pageRepo;
    @Mock ProcessingLogRepository procLogRepo;

    @InjectMocks
    MaintenanceService maintenanceService;

    @Test
    void findSplitSuggestionsShouldReturnPagesWithLongContent() {
        // Given - content longer than 10000 chars (200 * 50)
        String longContent = "a".repeat(10001);
        String shortContent = "short content";

        Page longPage = Page.builder()
                .id(UUID.randomUUID())
                .title("Long Page")
                .slug("long-page")
                .content(longContent)
                .build();

        Page shortPage = Page.builder()
                .id(UUID.randomUUID())
                .title("Short Page")
                .slug("short-page")
                .content(shortContent)
                .build();

        Page nullContentPage = Page.builder()
                .id(UUID.randomUUID())
                .title("Null Content Page")
                .slug("null-page")
                .content(null)
                .build();

        when(pageRepo.findAll()).thenReturn(List.of(longPage, shortPage, nullContentPage));

        // When
        List<Page> result = maintenanceService.findSplitSuggestions();

        // Then
        assertEquals(1, result.size());
        assertEquals("Long Page", result.get(0).getTitle());
        assertEquals(longContent, result.get(0).getContent());
    }

    @Test
    void findSplitSuggestionsShouldReturnEmptyWhenNoLongPages() {
        // Given - all pages have short content
        Page shortPage = Page.builder()
                .id(UUID.randomUUID())
                .title("Short Page")
                .slug("short-page")
                .content("short")
                .build();

        when(pageRepo.findAll()).thenReturn(List.of(shortPage));

        // When
        List<Page> result = maintenanceService.findSplitSuggestions();

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void findSplitSuggestionsShouldReturnEmptyForEmptyPageList() {
        // Given
        when(pageRepo.findAll()).thenReturn(new ArrayList<>());

        // When
        List<Page> result = maintenanceService.findSplitSuggestions();

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void findSplitSuggestionsBoundaryTest() {
        // Given - content exactly at threshold (10000 chars = NOT over threshold)
        String exactThreshold = "a".repeat(10000);
        Page boundaryPage = Page.builder()
                .id(UUID.randomUUID())
                .title("Boundary Page")
                .slug("boundary-page")
                .content(exactThreshold)
                .build();

        when(pageRepo.findAll()).thenReturn(List.of(boundaryPage));

        // When
        List<Page> result = maintenanceService.findSplitSuggestions();

        // Then - exactly 10000 chars should NOT be suggested for split (must be > 10000)
        assertTrue(result.isEmpty());
    }
}
