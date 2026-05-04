package com.llmwiki.service.maintenance;

import com.llmwiki.common.enums.NodeType;
import com.llmwiki.domain.graph.entity.KgNode;
import com.llmwiki.domain.graph.repository.KgEdgeRepository;
import com.llmwiki.domain.graph.repository.KgNodeRepository;
import com.llmwiki.domain.page.entity.Page;
import com.llmwiki.domain.page.repository.PageRepository;
import com.llmwiki.domain.processing.repository.ProcessingLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        String longContent = "a".repeat(10001);
        String shortContent = "short content";

        Page longPage = Page.builder()
                .id(UUID.randomUUID()).title("Long Page")
                .slug("long-page").content(longContent).build();
        Page shortPage = Page.builder()
                .id(UUID.randomUUID()).title("Short Page")
                .slug("short-page").content(shortContent).build();
        Page nullContentPage = Page.builder()
                .id(UUID.randomUUID()).title("Null Content Page")
                .slug("null-page").content(null).build();

        when(pageRepo.findAll()).thenReturn(List.of(longPage, shortPage, nullContentPage));

        List<Page> result = maintenanceService.findSplitSuggestions();

        assertEquals(1, result.size());
        assertEquals("Long Page", result.get(0).getTitle());
    }

    @Test
    void findSplitSuggestionsShouldReturnEmptyWhenNoLongPages() {
        Page shortPage = Page.builder()
                .id(UUID.randomUUID()).title("Short Page")
                .slug("short-page").content("short").build();
        when(pageRepo.findAll()).thenReturn(List.of(shortPage));

        assertTrue(maintenanceService.findSplitSuggestions().isEmpty());
    }

    @Test
    void findSplitSuggestionsShouldReturnEmptyForEmptyPageList() {
        when(pageRepo.findAll()).thenReturn(new ArrayList<>());
        assertTrue(maintenanceService.findSplitSuggestions().isEmpty());
    }

    @Test
    void findSplitSuggestionsBoundaryTest() {
        Page boundaryPage = Page.builder()
                .id(UUID.randomUUID()).title("Boundary Page")
                .slug("boundary-page").content("a".repeat(10000)).build();
        when(pageRepo.findAll()).thenReturn(List.of(boundaryPage));

        assertTrue(maintenanceService.findSplitSuggestions().isEmpty());
    }

    @Test
    void findDuplicatesShouldDetectExactDuplicates() {
        Page page1 = Page.builder()
                .id(UUID.randomUUID()).title("Java Guide")
                .slug("java-guide").content("Content 1").build();
        Page page2 = Page.builder()
                .id(UUID.randomUUID()).title("Java Guide")
                .slug("java-guide-2").content("Content 2").build();
        Page page3 = Page.builder()
                .id(UUID.randomUUID()).title("Python Tutorial")
                .slug("python-tutorial").content("Different content").build();

        when(pageRepo.findAll()).thenReturn(List.of(page1, page2, page3));

        List<DuplicateGroup> result = maintenanceService.findDuplicates();

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getPages().size());
        assertEquals(1.0, result.get(0).getSimilarity(), 0.001);
    }

    @Test
    void findDuplicatesShouldDetectNearDuplicates() {
        Page page1 = Page.builder()
                .id(UUID.randomUUID()).title("Java Programming Guide")
                .slug("java-guide").content("Content 1").build();
        Page page2 = Page.builder()
                .id(UUID.randomUUID()).title("Java Programming Guides")
                .slug("java-guides").content("Content 2").build();
        Page page3 = Page.builder()
                .id(UUID.randomUUID()).title("Python Tutorial")
                .slug("python-tutorial").content("Different content").build();

        when(pageRepo.findAll()).thenReturn(List.of(page1, page2, page3));

        List<DuplicateGroup> result = maintenanceService.findDuplicates();

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getPages().size());
        assertTrue(result.get(0).getSimilarity() > 0.85);
    }

    @Test
    void findDuplicatesShouldNotGroupDissimilarTitles() {
        Page page1 = Page.builder()
                .id(UUID.randomUUID()).title("Java Programming Guide")
                .slug("java-guide").content("Content 1").build();
        Page page2 = Page.builder()
                .id(UUID.randomUUID()).title("Python Data Science Handbook")
                .slug("python-handbook").content("Content 2").build();
        Page page3 = Page.builder()
                .id(UUID.randomUUID()).title("React Component Patterns")
                .slug("react-patterns").content("Content 3").build();

        when(pageRepo.findAll()).thenReturn(List.of(page1, page2, page3));

        assertTrue(maintenanceService.findDuplicates().isEmpty());
    }

    @Test
    void findDuplicatesShouldReturnEmptyForEmptyPageList() {
        when(pageRepo.findAll()).thenReturn(new ArrayList<>());
        assertTrue(maintenanceService.findDuplicates().isEmpty());
    }

    @Test
    void findDuplicatesShouldReturnEmptyForSinglePage() {
        Page page = Page.builder()
                .id(UUID.randomUUID()).title("Solo Page")
                .slug("solo").content("Content").build();
        when(pageRepo.findAll()).thenReturn(List.of(page));
        assertTrue(maintenanceService.findDuplicates().isEmpty());
    }

    @Test
    void findDuplicatesShouldBeCaseInsensitive() {
        Page page1 = Page.builder()
                .id(UUID.randomUUID()).title("JAVA GUIDE")
                .slug("java-guide-1").content("Content 1").build();
        Page page2 = Page.builder()
                .id(UUID.randomUUID()).title("java guide")
                .slug("java-guide-2").content("Content 2").build();
        when(pageRepo.findAll()).thenReturn(List.of(page1, page2));

        List<DuplicateGroup> result = maintenanceService.findDuplicates();

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getPages().size());
        assertEquals(1.0, result.get(0).getSimilarity(), 0.001);
    }

    // ===== Orphan detection tests (PR #29) =====

    @Test
    void findOrphansShouldReturnOrphanPages() {
        UUID connectedId = UUID.randomUUID();
        UUID orphanId = UUID.randomUUID();

        Page connectedPage = Page.builder()
                .id(connectedId).title("Connected Page")
                .slug("connected-page").content("Has edges").build();
        Page orphanPage = Page.builder()
                .id(orphanId).title("Orphan Page")
                .slug("orphan-page").content("No edges").build();

        when(kgEdgeRepo.findConnectedPageIds()).thenReturn(List.of(connectedId));
        when(pageRepo.findOrphanPages(List.of(connectedId))).thenReturn(List.of(orphanPage));

        List<Page> result = maintenanceService.findOrphans();

        assertEquals(1, result.size());
        assertEquals("Orphan Page", result.get(0).getTitle());
        assertEquals(orphanId, result.get(0).getId());

        verify(kgEdgeRepo).findConnectedPageIds();
        verify(pageRepo).findOrphanPages(List.of(connectedId));
        verifyNoMoreInteractions(kgEdgeRepo, pageRepo);
        verifyNoInteractions(kgNodeRepo);
    }

    @Test
    void findOrphansShouldReturnAllPagesWhenNoConnectionsExist() {
        Page page1 = Page.builder()
                .id(UUID.randomUUID()).title("Page One")
                .slug("page-one").content("Content").build();
        Page page2 = Page.builder()
                .id(UUID.randomUUID()).title("Page Two")
                .slug("page-two").content("Content").build();

        when(kgEdgeRepo.findConnectedPageIds()).thenReturn(List.of());
        when(pageRepo.findOrphanPages(List.of())).thenReturn(List.of(page1, page2));

        List<Page> result = maintenanceService.findOrphans();

        assertEquals(2, result.size());
        verify(kgEdgeRepo).findConnectedPageIds();
        verify(pageRepo).findOrphanPages(List.of());
    }

    @Test
    void findOrphansShouldReturnEmptyWhenAllPagesAreConnected() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        when(kgEdgeRepo.findConnectedPageIds()).thenReturn(List.of(id1, id2));
        when(pageRepo.findOrphanPages(List.of(id1, id2))).thenReturn(List.of());

        List<Page> result = maintenanceService.findOrphans();

        assertTrue(result.isEmpty());
        verify(kgEdgeRepo).findConnectedPageIds();
        verify(pageRepo).findOrphanPages(List.of(id1, id2));
    }

    @Test
    void findOrphansShouldNotCallFindAllOnEdgeOrPageRepos() {
        when(kgEdgeRepo.findConnectedPageIds()).thenReturn(List.of());
        when(pageRepo.findOrphanPages(List.of())).thenReturn(List.of());

        maintenanceService.findOrphans();

        verify(kgEdgeRepo, never()).findAll();
        verify(pageRepo, never()).findAll();
        verify(kgNodeRepo, never()).findAll();
    }

    @Test
    void findStalePagesShouldReturnPagesOlderThanCutoff() {
        Page stalePage = Page.builder()
                .id(UUID.randomUUID()).title("Old Page")
                .slug("old-page").content("Old content")
                .updatedAt(Instant.now().minus(60, ChronoUnit.DAYS))
                .build();
        Page recentPage = Page.builder()
                .id(UUID.randomUUID()).title("Recent Page")
                .slug("recent-page").content("Recent content")
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();

        when(pageRepo.findAll()).thenReturn(List.of(stalePage, recentPage));

        List<Page> result = maintenanceService.findStalePages(30);

        assertEquals(1, result.size());
        assertEquals("Old Page", result.get(0).getTitle());
    }

    @Test
    void findStalePagesShouldReturnEmptyForNoStalePages() {
        Page recentPage = Page.builder()
                .id(UUID.randomUUID()).title("Recent")
                .slug("recent").content("content")
                .updatedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();

        when(pageRepo.findAll()).thenReturn(List.of(recentPage));

        assertTrue(maintenanceService.findStalePages(30).isEmpty());
    }

    @Test
    void findStalePagesShouldHandleNullUpdatedAt() {
        Page nullTimePage = Page.builder()
                .id(UUID.randomUUID()).title("Null Time")
                .slug("null-time").content("content")
                .updatedAt(null)
                .build();

        when(pageRepo.findAll()).thenReturn(List.of(nullTimePage));

        assertTrue(maintenanceService.findStalePages(30).isEmpty());
    }

    @Test
    void findContradictionsShouldReturnContestedPages() {
        Page contestedPage = Page.builder()
                .id(UUID.randomUUID()).title("Contested")
                .slug("contested").content("content")
                .contested(true)
                .build();
        Page normalPage = Page.builder()
                .id(UUID.randomUUID()).title("Normal")
                .slug("normal").content("content")
                .contested(false)
                .build();
        Page nullContestedPage = Page.builder()
                .id(UUID.randomUUID()).title("Null Contested")
                .slug("null-contested").content("content")
                .contested(null)
                .build();

        when(pageRepo.findAll()).thenReturn(List.of(contestedPage, normalPage, nullContestedPage));

        List<Page> result = maintenanceService.findContradictions();

        assertEquals(1, result.size());
        assertEquals("Contested", result.get(0).getTitle());
    }

    @Test
    void findContradictionsShouldReturnEmptyWhenNoContestedPages() {
        Page normalPage = Page.builder()
                .id(UUID.randomUUID()).title("Normal")
                .slug("normal").content("content")
                .contested(false)
                .build();

        when(pageRepo.findAll()).thenReturn(List.of(normalPage));

        assertTrue(maintenanceService.findContradictions().isEmpty());
    }

    @Test
    void indexConsistencyCheckShouldFindPagesWithoutNodes() {
        UUID pageWithNodeId = UUID.randomUUID();
        UUID pageWithoutNodeId = UUID.randomUUID();

        KgNode node = KgNode.builder()
                .id(UUID.randomUUID()).name("Node")
                .nodeType(com.llmwiki.common.enums.NodeType.ENTITY)
                .pageId(pageWithNodeId)
                .build();
        Page pageWithNode = Page.builder()
                .id(pageWithNodeId).title("Has Node")
                .slug("has-node").content("content")
                .build();
        Page pageWithoutNode = Page.builder()
                .id(pageWithoutNodeId).title("No Node")
                .slug("no-node").content("content")
                .build();

        when(kgNodeRepo.findAll()).thenReturn(List.of(node));
        when(pageRepo.findAll()).thenReturn(List.of(pageWithNode, pageWithoutNode));
        when(pageRepo.count()).thenReturn(2L);

        Map<String, Object> result = maintenanceService.indexConsistencyCheck();

        assertEquals(2L, result.get("totalPages"));
        assertEquals(1, result.get("pagesWithoutNodes"));
        List<String> orphanPages = (List<String>) result.get("orphanPages");
        assertEquals(1, orphanPages.size());
        assertEquals("No Node", orphanPages.get(0));
    }

    @Test
    void indexConsistencyCheckShouldReturnAllMatchedWhenEveryPageHasNode() {
        UUID pageId = UUID.randomUUID();
        KgNode node = KgNode.builder()
                .id(UUID.randomUUID()).name("Node")
                .nodeType(com.llmwiki.common.enums.NodeType.ENTITY)
                .pageId(pageId)
                .build();
        Page page = Page.builder()
                .id(pageId).title("Page")
                .slug("page").content("content")
                .build();

        when(kgNodeRepo.findAll()).thenReturn(List.of(node));
        when(pageRepo.findAll()).thenReturn(List.of(page));
        when(pageRepo.count()).thenReturn(1L);

        Map<String, Object> result = maintenanceService.indexConsistencyCheck();

        assertEquals(1L, result.get("totalPages"));
        assertEquals(0, result.get("pagesWithoutNodes"));
        assertTrue(((List<?>) result.get("orphanPages")).isEmpty());
    }

    @Test
    void indexConsistencyCheckShouldHandleNodesWithNullPageId() {
        KgNode nodeWithNullPageId = KgNode.builder()
                .id(UUID.randomUUID()).name("No Page Node")
                .nodeType(com.llmwiki.common.enums.NodeType.ENTITY)
                .pageId(null)
                .build();
        Page page = Page.builder()
                .id(UUID.randomUUID()).title("Orphan Page")
                .slug("orphan-page").content("content")
                .build();

        when(kgNodeRepo.findAll()).thenReturn(List.of(nodeWithNullPageId));
        when(pageRepo.findAll()).thenReturn(List.of(page));
        when(pageRepo.count()).thenReturn(1L);

        Map<String, Object> result = maintenanceService.indexConsistencyCheck();

        assertEquals(1, result.get("pagesWithoutNodes"));
    }

    @Test
    void generateReportShouldAggregateAllStats() {
        when(pageRepo.count()).thenReturn(10L);
        when(kgEdgeRepo.findConnectedPageIds()).thenReturn(List.of());
        when(pageRepo.findOrphanPages(List.of())).thenReturn(List.of());
        when(pageRepo.findAll()).thenReturn(List.of());

        MaintenanceReport report = maintenanceService.generateReport();

        assertNotNull(report.getGeneratedAt());
        assertEquals(10L, report.getTotalPages());
        assertEquals(0, report.getOrphanCount());
        assertEquals(0, report.getStaleCount());
        assertEquals(0, report.getDuplicateGroups());
        assertEquals(0, report.getContradictionCount());
    }
}
