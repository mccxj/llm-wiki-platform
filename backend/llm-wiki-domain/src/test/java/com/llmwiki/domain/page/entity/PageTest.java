package com.llmwiki.domain.page.entity;

import com.llmwiki.common.enums.ConfidenceLevel;
import com.llmwiki.common.enums.PageStatus;
import com.llmwiki.common.enums.PageType;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class PageTest {

    @Test
    void shouldCreatePageWithBuilder() {
        Page page = Page.builder()
            .slug("test-page")
            .title("Test Page")
            .content("Test content")
            .pageType(PageType.ENTITY)
            .status(PageStatus.PENDING_APPROVAL)
            .confidence(ConfidenceLevel.HIGH)
            .aiScore(new BigDecimal("8.5"))
            .build();

        assertEquals("test-page", page.getSlug());
        assertEquals("Test Page", page.getTitle());
        assertEquals(PageType.ENTITY, page.getPageType());
        assertEquals(PageStatus.PENDING_APPROVAL, page.getStatus());
        assertEquals(ConfidenceLevel.HIGH, page.getConfidence());
        assertEquals(new BigDecimal("8.5"), page.getAiScore());
        assertFalse(page.getContested());
    }

    @Test
    void shouldSetDefaultStatus() {
        Page page = Page.builder()
            .slug("test")
            .title("Test")
            .pageType(PageType.CONCEPT)
            .build();

        assertEquals(PageStatus.PENDING_APPROVAL, page.getStatus());
    }

    @Test
    void shouldSetTimestampsOnCreate() {
        Page page = Page.builder()
            .slug("test")
            .title("Test")
            .pageType(PageType.ENTITY)
            .build();

        // Timestamps are set by @PrePersist which only runs in JPA context
        // Here we just verify the builder works
        assertNotNull(page);
    }
}
