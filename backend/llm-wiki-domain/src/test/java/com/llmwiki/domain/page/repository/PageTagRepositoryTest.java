package com.llmwiki.domain.page.repository;

import com.llmwiki.domain.page.entity.PageTag;
import com.llmwiki.domain.page.entity.PageTagId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PageTagRepositoryTest {

    @Autowired
    PageTagRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveAndFindById() {
        UUID pageId = UUID.randomUUID();
        PageTag tag = PageTag.builder()
                .pageId(pageId)
                .tag("programming")
                .build();

        PageTag saved = repository.save(tag);
        assertEquals(pageId, saved.getPageId());
        assertEquals("programming", saved.getTag());

        PageTagId id = new PageTagId(pageId, "programming");
        PageTag found = repository.findById(id).orElseThrow();
        assertEquals(pageId, found.getPageId());
        assertEquals("programming", found.getTag());
    }

    @Test
    void shouldFindByPageId() {
        UUID pageId = UUID.randomUUID();
        repository.save(PageTag.builder().pageId(pageId).tag("java").build());
        repository.save(PageTag.builder().pageId(pageId).tag("programming").build());
        repository.save(PageTag.builder().pageId(UUID.randomUUID()).tag("other").build());

        List<PageTag> results = repository.findByPageId(pageId);
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(t -> t.getPageId().equals(pageId)));
    }

    @Test
    void shouldFindByTagIgnoreCase() {
        repository.save(PageTag.builder().pageId(UUID.randomUUID()).tag("Java").build());
        repository.save(PageTag.builder().pageId(UUID.randomUUID()).tag("java").build());
        repository.save(PageTag.builder().pageId(UUID.randomUUID()).tag("JAVA").build());
        repository.save(PageTag.builder().pageId(UUID.randomUUID()).tag("python").build());

        List<PageTag> results = repository.findByTagIgnoreCase("java");
        assertEquals(3, results.size());

        List<PageTag> python = repository.findByTagIgnoreCase("PYTHON");
        assertEquals(1, python.size());
    }

    @Test
    void shouldReturnEmptyListForUnknownPageId() {
        List<PageTag> results = repository.findByPageId(UUID.randomUUID());
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldReturnEmptyListForUnknownTag() {
        List<PageTag> results = repository.findByTagIgnoreCase("nonexistent");
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldSupportMultipleTagsPerPage() {
        UUID pageId = UUID.randomUUID();
        repository.save(PageTag.builder().pageId(pageId).tag("java").build());
        repository.save(PageTag.builder().pageId(pageId).tag("oop").build());
        repository.save(PageTag.builder().pageId(pageId).tag("programming").build());

        List<PageTag> results = repository.findByPageId(pageId);
        assertEquals(3, results.size());
    }

    @Test
    void shouldSupportSameTagOnMultiplePages() {
        String tag = "java";
        repository.save(PageTag.builder().pageId(UUID.randomUUID()).tag(tag).build());
        repository.save(PageTag.builder().pageId(UUID.randomUUID()).tag(tag).build());
        repository.save(PageTag.builder().pageId(UUID.randomUUID()).tag(tag).build());

        List<PageTag> results = repository.findByTagIgnoreCase(tag);
        assertEquals(3, results.size());
    }
}
