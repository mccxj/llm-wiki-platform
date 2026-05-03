package com.llmwiki.service.maintenance;

import com.llmwiki.domain.page.entity.Page;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DuplicateGroupTest {

    @Test
    void shouldCreateWithNoArgs() {
        DuplicateGroup group = new DuplicateGroup();
        assertNull(group.getPages());
        assertEquals(0.0, group.getSimilarity());
    }

    @Test
    void shouldCreateWithAllArgs() {
        List<Page> pages = List.of(
                Page.builder().title("Page A").build(),
                Page.builder().title("Page B").build());
        DuplicateGroup group = new DuplicateGroup(pages, 0.92);

        assertEquals(2, group.getPages().size());
        assertEquals(0.92, group.getSimilarity());
    }

    @Test
    void shouldSetFields() {
        DuplicateGroup group = new DuplicateGroup();
        List<Page> pages = List.of(Page.builder().title("Test").build());
        group.setPages(pages);
        group.setSimilarity(0.85);

        assertEquals(pages, group.getPages());
        assertEquals(0.85, group.getSimilarity());
    }
}
