package com.llmwiki.service.pipeline;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ConsistencyReportTest {

    @Test
    void shouldCreatePassedReport() {
        ConsistencyReport report = ConsistencyReport.builder()
                .passed(true)
                .issues(List.of())
                .entityCount(5)
                .linkedPagesCount(3)
                .build();

        assertTrue(report.isPassed());
        assertTrue(report.getIssues().isEmpty());
        assertEquals(5, report.getEntityCount());
        assertEquals(3, report.getLinkedPagesCount());
    }

    @Test
    void shouldCreateFailedReport() {
        ConsistencyReport report = ConsistencyReport.builder()
                .passed(false)
                .issues(List.of("Title is empty", "Content too short"))
                .entityCount(0)
                .linkedPagesCount(0)
                .build();

        assertFalse(report.isPassed());
        assertEquals(2, report.getIssues().size());
        assertEquals("Title is empty", report.getIssues().get(0));
        assertEquals("Content too short", report.getIssues().get(1));
    }

    @Test
    void shouldSupportSetters() {
        ConsistencyReport report = ConsistencyReport.builder()
                .passed(true)
                .issues(List.of())
                .entityCount(2)
                .linkedPagesCount(1)
                .build();

        report.setPassed(false);
        report.setIssues(List.of("issue1"));
        report.setEntityCount(3);
        report.setLinkedPagesCount(2);

        assertFalse(report.isPassed());
        assertEquals(1, report.getIssues().size());
        assertEquals(3, report.getEntityCount());
        assertEquals(2, report.getLinkedPagesCount());
    }
}
