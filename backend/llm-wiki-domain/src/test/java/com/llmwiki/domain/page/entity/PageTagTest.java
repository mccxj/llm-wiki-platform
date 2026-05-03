package com.llmwiki.domain.page.entity;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PageTagTest {

    @Test
    void shouldCreateWithBuilder() {
        UUID pageId = UUID.randomUUID();
        PageTag tag = PageTag.builder()
                .pageId(pageId)
                .tag("java")
                .build();

        assertEquals(pageId, tag.getPageId());
        assertEquals("java", tag.getTag());
    }

    @Test
    void shouldSupportNoArgsConstructor() {
        PageTag tag = new PageTag();
        tag.setPageId(UUID.randomUUID());
        tag.setTag("spring");

        assertNotNull(tag.getPageId());
        assertEquals("spring", tag.getTag());
    }

    @Test
    void pageTagIdShouldBeSerializable() {
        PageTagId id = new PageTagId(UUID.randomUUID(), "test-tag");
        assertNotNull(id.getPageId());
        assertEquals("test-tag", id.getTag());

        PageTagId emptyId = new PageTagId();
        assertNull(emptyId.getPageId());
        assertNull(emptyId.getTag());
    }

    @Test
    void pageTagIdShouldSupportEquals() {
        UUID pageId = UUID.randomUUID();
        PageTagId id1 = new PageTagId(pageId, "tag");
        PageTagId id2 = new PageTagId(pageId, "tag");
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }
}
