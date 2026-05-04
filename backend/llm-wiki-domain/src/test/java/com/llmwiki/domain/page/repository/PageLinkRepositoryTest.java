package com.llmwiki.domain.page.repository;

import com.llmwiki.common.enums.EdgeType;
import com.llmwiki.domain.page.entity.PageLink;
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
class PageLinkRepositoryTest {

    @Autowired
    PageLinkRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveAndFindById() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        PageLink link = PageLink.builder()
                .sourcePageId(sourceId)
                .targetPageId(targetId)
                .linkType(EdgeType.RELATED_TO)
                .build();

        PageLink saved = repository.save(link);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        PageLink found = repository.findById(saved.getId()).orElseThrow();
        assertEquals(sourceId, found.getSourcePageId());
        assertEquals(targetId, found.getTargetPageId());
        assertEquals(EdgeType.RELATED_TO, found.getLinkType());
    }

    @Test
    void shouldFindBySourcePageId() {
        UUID sourceId = UUID.randomUUID();
        repository.save(PageLink.builder().sourcePageId(sourceId).targetPageId(UUID.randomUUID()).build());
        repository.save(PageLink.builder().sourcePageId(sourceId).targetPageId(UUID.randomUUID()).build());
        repository.save(PageLink.builder().sourcePageId(UUID.randomUUID()).targetPageId(UUID.randomUUID()).build());

        List<PageLink> results = repository.findBySourcePageId(sourceId);
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(l -> l.getSourcePageId().equals(sourceId)));
    }

    @Test
    void shouldFindByTargetPageId() {
        UUID targetId = UUID.randomUUID();
        repository.save(PageLink.builder().sourcePageId(UUID.randomUUID()).targetPageId(targetId).build());
        repository.save(PageLink.builder().sourcePageId(UUID.randomUUID()).targetPageId(targetId).build());
        repository.save(PageLink.builder().sourcePageId(UUID.randomUUID()).targetPageId(UUID.randomUUID()).build());

        List<PageLink> results = repository.findByTargetPageId(targetId);
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(l -> l.getTargetPageId().equals(targetId)));
    }

    @Test
    void shouldReturnEmptyListForUnknownSourcePageId() {
        List<PageLink> results = repository.findBySourcePageId(UUID.randomUUID());
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldReturnEmptyListForUnknownTargetPageId() {
        List<PageLink> results = repository.findByTargetPageId(UUID.randomUUID());
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldDefaultLinkTypeToRelatedTo() {
        PageLink link = PageLink.builder()
                .sourcePageId(UUID.randomUUID())
                .targetPageId(UUID.randomUUID())
                .build();

        PageLink saved = repository.save(link);
        assertEquals(EdgeType.RELATED_TO, saved.getLinkType());
    }

    @Test
    void shouldSupportDifferentLinkTypes() {
        repository.save(PageLink.builder()
                .sourcePageId(UUID.randomUUID()).targetPageId(UUID.randomUUID())
                .linkType(EdgeType.IS_A).build());
        repository.save(PageLink.builder()
                .sourcePageId(UUID.randomUUID()).targetPageId(UUID.randomUUID())
                .linkType(EdgeType.DEPENDS_ON).build());

        List<PageLink> all = repository.findAll();
        assertEquals(2, all.size());
    }
}
