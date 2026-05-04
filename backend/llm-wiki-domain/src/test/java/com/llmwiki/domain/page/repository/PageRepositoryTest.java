package com.llmwiki.domain.page.repository;

import com.llmwiki.common.enums.PageStatus;
import com.llmwiki.common.enums.PageType;
import com.llmwiki.domain.page.entity.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PageRepositoryTest {

    @Autowired
    PageRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveAndFindById() {
        Page page = Page.builder()
                .slug("test-page")
                .title("Test Page")
                .content("# Test Content")
                .pageType(PageType.ENTITY)
                .status(PageStatus.PENDING_APPROVAL)
                .build();

        Page saved = repository.save(page);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        Page found = repository.findById(saved.getId()).orElseThrow();
        assertEquals("test-page", found.getSlug());
        assertEquals("Test Page", found.getTitle());
        assertEquals(PageType.ENTITY, found.getPageType());
        assertEquals(PageStatus.PENDING_APPROVAL, found.getStatus());
    }

    @Test
    void shouldFindBySlug() {
        repository.save(Page.builder().slug("java-page").title("Java").pageType(PageType.ENTITY).build());

        Optional<Page> found = repository.findBySlug("java-page");
        assertTrue(found.isPresent());
        assertEquals("Java", found.get().getTitle());

        Optional<Page> missing = repository.findBySlug("nonexistent");
        assertFalse(missing.isPresent());
    }

    @Test
    void shouldFindByPageType() {
        repository.save(Page.builder().slug("e1").title("E1").pageType(PageType.ENTITY).build());
        repository.save(Page.builder().slug("e2").title("E2").pageType(PageType.ENTITY).build());
        repository.save(Page.builder().slug("c1").title("C1").pageType(PageType.CONCEPT).build());

        List<Page> entities = repository.findByPageType(PageType.ENTITY);
        assertEquals(2, entities.size());

        List<Page> concepts = repository.findByPageType(PageType.CONCEPT);
        assertEquals(1, concepts.size());
    }

    @Test
    void shouldFindByStatusOrderByCreatedAtDesc() {
        repository.save(Page.builder().slug("p1").title("P1").pageType(PageType.ENTITY).status(PageStatus.APPROVED).build());
        repository.save(Page.builder().slug("p2").title("P2").pageType(PageType.ENTITY).status(PageStatus.APPROVED).build());
        repository.save(Page.builder().slug("p3").title("P3").pageType(PageType.ENTITY).status(PageStatus.ARCHIVED).build());

        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Page> approved = repository.findByStatusOrderByCreatedAtDesc(PageStatus.APPROVED, pageable);
        assertEquals(2, approved.size());

        List<Page> archived = repository.findByStatusOrderByCreatedAtDesc(PageStatus.ARCHIVED, pageable);
        assertEquals(1, archived.size());
    }

    @Test
    void shouldFindByPageTypeWithPagination() {
        repository.save(Page.builder().slug("e1").title("E1").pageType(PageType.ENTITY).build());

        var pageable = PageRequest.of(0, 20);
        List<Page> results = repository.findByPageType(PageType.ENTITY, pageable);
        assertEquals(1, results.size());
    }

    @Test
    void shouldFindByStatusOrderByCreatedAtDescWithPagination() {
        repository.save(Page.builder().slug("p1").title("P1").pageType(PageType.ENTITY).status(PageStatus.APPROVED).build());
        repository.save(Page.builder().slug("p2").title("P2").pageType(PageType.ENTITY).status(PageStatus.APPROVED).build());

        var pageable = PageRequest.of(0, 20);
        List<Page> results = repository.findByStatusOrderByCreatedAtDesc(PageStatus.APPROVED, pageable);
        assertEquals(2, results.size());
    }

    @Test
    void shouldFindOrphanPages() {
        Page connected = repository.save(Page.builder().slug("connected").title("Connected").pageType(PageType.ENTITY).build());
        repository.save(Page.builder().slug("orphan").title("Orphan").pageType(PageType.ENTITY).build());

        List<Page> orphans = repository.findOrphanPages(List.of(connected.getId()));
        assertEquals(1, orphans.size());
        assertEquals("orphan", orphans.get(0).getSlug());
    }

    @Test
    void shouldDefaultStatusToPendingApproval() {
        Page page = Page.builder()
                .slug("new-page")
                .title("New")
                .pageType(PageType.ENTITY)
                .build();

        Page saved = repository.save(page);
        assertEquals(PageStatus.PENDING_APPROVAL, saved.getStatus());
    }

    @Test
    void shouldHandleAllFields() {
        UUID sourceDocId = UUID.randomUUID();
        Page page = Page.builder()
                .slug("full-page")
                .title("Full Page")
                .content("Full content")
                .pageType(PageType.COMPARISON)
                .status(PageStatus.APPROVED)
                .confidence(com.llmwiki.common.enums.ConfidenceLevel.HIGH)
                .contested(false)
                .aiScore(new java.math.BigDecimal("8.5"))
                .aiScoreDetail("{\"relevance\":9}")
                .sourceDocId(sourceDocId)
                .approvedBy("admin")
                .approvedAt(java.time.Instant.now())
                .build();

        Page saved = repository.save(page);
        Page found = repository.findById(saved.getId()).orElseThrow();
        assertEquals(PageType.COMPARISON, found.getPageType());
        assertEquals(com.llmwiki.common.enums.ConfidenceLevel.HIGH, found.getConfidence());
        assertFalse(found.getContested());
        assertEquals(sourceDocId, found.getSourceDocId());
        assertEquals("admin", found.getApprovedBy());
    }
}
