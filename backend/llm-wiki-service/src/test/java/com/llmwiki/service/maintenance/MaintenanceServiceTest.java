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

    @Test
    void findDuplicatesShouldDetectExactDuplicates() {
        // Given - two pages with the same title
        Page page1 = Page.builder()
                .id(UUID.randomUUID())
                .title("Java Guide")
                .slug("java-guide")
                .content("Content 1")
                .build();

        Page page2 = Page.builder()
                .id(UUID.randomUUID())
                .title("Java Guide")
                .slug("java-guide-2")
                .content("Content 2")
                .build();

        Page page3 = Page.builder()
                .id(UUID.randomUUID())
                .title("Python Tutorial")
                .slug("python-tutorial")
                .content("Different content")
                .build();

        when(pageRepo.findAll()).thenReturn(List.of(page1, page2, page3));

        // When
        List<DuplicateGroup> result = maintenanceService.findDuplicates();

        // Then - should find one group with the two "Java Guide" pages
        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getPages().size());
        assertEquals(1.0, result.get(0).getSimilarity(), 0.001);
        List<String> titles = result.get(0).getPages().stream()
                .map(Page::getTitle)
                .toList();
        assertTrue(titles.contains("Java Guide"));
    }

    @Test
    void findDuplicatesShouldDetectNearDuplicates() {
        // Given - titles that are similar but not identical
        Page page1 = Page.builder()
                .id(UUID.randomUUID())
                .title("Java Programming Guide")
                .slug("java-guide")
                .content("Content 1")
                .build();

        Page page2 = Page.builder()
                .id(UUID.randomUUID())
                .title("Java Programming Guides")
                .slug("java-guides")
                .content("Content 2")
                .build();

        Page page3 = Page.builder()
                .id(UUID.randomUUID())
                .title("Python Tutorial")
                .slug("python-tutorial")
                .content("Different content")
                .build();

        when(pageRepo.findAll()).thenReturn(List.of(page1, page2, page3));

        // When
        List<DuplicateGroup> result = maintenanceService.findDuplicates();

        // Then - should detect near-duplicate group (Jaro-Winkler > 0.85)
        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getPages().size());
        assertTrue(result.get(0).getSimilarity() > 0.85);
    }

    @Test
    void findDuplicatesShouldNotGroupDissimilarTitles() {
        // Given - pages with very different titles
        Page page1 = Page.builder()
                .id(UUID.randomUUID())
                .title("Java Programming Guide")
                .slug("java-guide")
                .content("Content 1")
                .build();

        Page page2 = Page.builder()
                .id(UUID.randomUUID())
                .title("Python Data Science Handbook")
                .slug("python-handbook")
                .content("Content 2")
                .build();

        Page page3 = Page.builder()
                .id(UUID.randomUUID())
                .title("React Component Patterns")
                .slug("react-patterns")
                .content("Content 3")
                .build();

        when(pageRepo.findAll()).thenReturn(List.of(page1, page2, page3));

        // When
        List<DuplicateGroup> result = maintenanceService.findDuplicates();

        // Then - no groups should be found
        assertTrue(result.isEmpty());
    }

    @Test
    void findDuplicatesShouldReturnEmptyForEmptyPageList() {
        // Given
        when(pageRepo.findAll()).thenReturn(new ArrayList<>());

        // When
        List<DuplicateGroup> result = maintenanceService.findDuplicates();

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void findDuplicatesShouldReturnEmptyForSinglePage() {
        // Given - only one page, no duplicates possible
        Page page = Page.builder()
                .id(UUID.randomUUID())
                .title("Solo Page")
                .slug("solo")
                .content("Content")
                .build();

        when(pageRepo.findAll()).thenReturn(List.of(page));

        // When
        List<DuplicateGroup> result = maintenanceService.findDuplicates();

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void findDuplicatesShouldBeCaseInsensitive() {
        // Given - same title different cases
        Page page1 = Page.builder()
                .id(UUID.randomUUID())
                .title("JAVA GUIDE")
                .slug("java-guide-1")
                .content("Content 1")
                .build();

        Page page2 = Page.builder()
                .id(UUID.randomUUID())
                .title("java guide")
                .slug("java-guide-2")
                .content("Content 2")
                .build();

        when(pageRepo.findAll()).thenReturn(List.of(page1, page2));

        // When
        List<DuplicateGroup> result = maintenanceService.findDuplicates();

        // Then - should detect as duplicates (case-insensitive)
        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getPages().size());
        assertEquals(1.0, result.get(0).getSimilarity(), 0.001);
    }
}
