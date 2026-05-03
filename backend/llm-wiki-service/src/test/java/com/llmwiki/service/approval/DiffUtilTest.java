package com.llmwiki.service.approval;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DiffUtilTest {

    @Test
    void generateDiff_shouldShowInsertions() {
        String before = "line1\nline2\n";
        String after = "line1\nline2\nline3\n";

        String diff = DiffUtil.generateDiff(before, after);

        assertTrue(diff.contains("+ "));
        assertTrue(diff.contains("line3"));
    }

    @Test
    void generateDiff_shouldShowDeletions() {
        String before = "line1\nline2\nline3\n";
        String after = "line1\nline3\n";

        String diff = DiffUtil.generateDiff(before, after);

        assertTrue(diff.contains("- "));
        assertTrue(diff.contains("line2"));
    }

    @Test
    void generateDiff_shouldShowChanges() {
        String before = "line1\nold line\n";
        String after = "line1\nnew line\n";

        String diff = DiffUtil.generateDiff(before, after);

        assertTrue(diff.contains("- "));
        assertTrue(diff.contains("+ "));
        assertTrue(diff.contains("old line"));
        assertTrue(diff.contains("new line"));
    }

    @Test
    void generateDiff_shouldHandleNullBefore() {
        String diff = DiffUtil.generateDiff(null, "new content\n");
        assertTrue(diff.contains("+ "));
    }

    @Test
    void generateDiff_shouldHandleNullAfter() {
        String diff = DiffUtil.generateDiff("old content\n", null);
        assertTrue(diff.contains("- "));
    }

    @Test
    void generateDiff_shouldHandleBothNull() {
        String diff = DiffUtil.generateDiff(null, null);
        assertEquals("", diff);
    }

    @Test
    void generateDiff_shouldHandleIdenticalContent() {
        String diff = DiffUtil.generateDiff("same\n", "same\n");
        assertEquals("", diff);
    }

    @Test
    void generateUnifiedDiff_shouldReturnUnifiedFormat() {
        String before = "line1\nline2\n";
        String after = "line1\nline3\n";

        String diff = DiffUtil.generateUnifiedDiff(before, after);

        assertNotNull(diff);
        assertTrue(diff.contains("--- before"));
        assertTrue(diff.contains("+++ after"));
    }

    @Test
    void generateUnifiedDiff_shouldHandleNullInputs() {
        String diff = DiffUtil.generateUnifiedDiff(null, null);
        assertNotNull(diff);
    }

    @Test
    void generateHtmlDiff_shouldReturnHtml() {
        String before = "line1\nold\n";
        String after = "line1\nnew\n";

        String diff = DiffUtil.generateHtmlDiff(before, after);

        assertTrue(diff.contains("<div class=\"diff\">"));
        assertTrue(diff.contains("<del>"));
        assertTrue(diff.contains("<ins>"));
        assertTrue(diff.contains("</div>"));
    }

    @Test
    void generateHtmlDiff_shouldHandleNullInputs() {
        String diff = DiffUtil.generateHtmlDiff(null, null);
        assertTrue(diff.contains("<div class=\"diff\">"));
    }

    @Test
    void generateHtmlDiff_shouldHandleIdenticalContent() {
        String diff = DiffUtil.generateHtmlDiff("same\n", "same\n");
        assertFalse(diff.contains("<del>"));
        assertFalse(diff.contains("<ins>"));
    }

    @Test
    void generateHtmlDiff_shouldEscapeHtmlEntities() {
        String before = "<script>alert('xss')</script>\n";
        String after = "<div>safe</div>\n";

        String diff = DiffUtil.generateHtmlDiff(before, after);

        assertFalse(diff.contains("<script>"));
        assertTrue(diff.contains("&lt;script&gt;"));
    }
}
