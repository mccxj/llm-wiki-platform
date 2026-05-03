package com.llmwiki.service.maintenance;

import com.llmwiki.domain.page.entity.Page;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MaintenanceReportTest {

    @Test
    void shouldCreateReportWithDefaults() {
        MaintenanceReport report = new MaintenanceReport();

        assertNull(report.getGeneratedAt());
        assertEquals(0L, report.getTotalPages());
        assertEquals(0, report.getOrphanCount());
        assertEquals(0, report.getStaleCount());
        assertEquals(0, report.getDuplicateGroups());
        assertEquals(0, report.getContradictionCount());
        assertNull(report.getOrphans());
        assertNull(report.getStalePages());
        assertNull(report.getDuplicates());
        assertNull(report.getContradictions());
    }

    @Test
    void shouldSetAllFields() {
        Instant now = Instant.now();
        List<Page> orphans = List.of(Page.builder().title("Orphan1").build());
        List<Page> stalePages = List.of(Page.builder().title("Stale1").build());
        List<DuplicateGroup> duplicates = List.of(new DuplicateGroup(List.of(), 0.9));
        List<Page> contradictions = List.of(Page.builder().title("Contested").build());

        MaintenanceReport report = new MaintenanceReport();
        report.setGeneratedAt(now);
        report.setTotalPages(100L);
        report.setOrphanCount(5);
        report.setStaleCount(10);
        report.setDuplicateGroups(3);
        report.setContradictionCount(2);
        report.setOrphans(orphans);
        report.setStalePages(stalePages);
        report.setDuplicates(duplicates);
        report.setContradictions(contradictions);

        assertEquals(now, report.getGeneratedAt());
        assertEquals(100L, report.getTotalPages());
        assertEquals(5, report.getOrphanCount());
        assertEquals(10, report.getStaleCount());
        assertEquals(3, report.getDuplicateGroups());
        assertEquals(2, report.getContradictionCount());
        assertEquals(orphans, report.getOrphans());
        assertEquals(stalePages, report.getStalePages());
        assertEquals(duplicates, report.getDuplicates());
        assertEquals(contradictions, report.getContradictions());
    }
}
