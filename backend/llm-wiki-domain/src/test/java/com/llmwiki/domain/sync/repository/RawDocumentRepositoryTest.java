package com.llmwiki.domain.sync.repository;

import com.llmwiki.domain.sync.entity.RawDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class RawDocumentRepositoryTest {

    @Autowired
    RawDocumentRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveAndFindById() {
        RawDocument doc = RawDocument.builder()
                .sourceId("src-1")
                .sourceName("test-wiki")
                .title("Test Document")
                .content("Test content")
                .contentHash("abc123")
                .build();

        RawDocument saved = repository.save(doc);
        assertNotNull(saved.getId());
        assertNotNull(saved.getIngestedAt());

        RawDocument found = repository.findById(saved.getId()).orElseThrow();
        assertEquals("src-1", found.getSourceId());
        assertEquals("test-wiki", found.getSourceName());
        assertEquals("Test Document", found.getTitle());
    }

    @Test
    void shouldFindBySourceId() {
        repository.save(RawDocument.builder()
                .sourceId("page-1").sourceName("wiki").title("T").content("C").contentHash("h").build());

        Optional<RawDocument> found = repository.findBySourceId("page-1");
        assertTrue(found.isPresent());
        assertEquals("page-1", found.get().getSourceId());

        Optional<RawDocument> missing = repository.findBySourceId("nonexistent");
        assertFalse(missing.isPresent());
    }

    @Test
    void shouldFindBySourceIdAndSourceName() {
        repository.save(RawDocument.builder()
                .sourceId("doc-1").sourceName("wiki-a").title("T").content("C").contentHash("h").build());
        repository.save(RawDocument.builder()
                .sourceId("doc-1").sourceName("wiki-b").title("T2").content("C2").contentHash("h2").build());

        Optional<RawDocument> found = repository.findBySourceIdAndSourceName("doc-1", "wiki-a");
        assertTrue(found.isPresent());
        assertEquals("wiki-a", found.get().getSourceName());

        Optional<RawDocument> notFound = repository.findBySourceIdAndSourceName("doc-1", "wiki-c");
        assertFalse(notFound.isPresent());
    }

    @Test
    void shouldReturnEmptyForUnknownSourceId() {
        Optional<RawDocument> found = repository.findBySourceId("nonexistent");
        assertFalse(found.isPresent());
    }

    @Test
    void shouldReturnEmptyForUnknownSourceIdAndSourceName() {
        Optional<RawDocument> found = repository.findBySourceIdAndSourceName("nonexistent", "wiki");
        assertFalse(found.isPresent());
    }

    @Test
    void shouldUpdateDocument() {
        RawDocument doc = repository.save(RawDocument.builder()
                .sourceId("src-1").sourceName("wiki").title("Old Title").content("Old").contentHash("old-hash").build());

        doc.setTitle("New Title");
        doc.setContent("New content");
        doc.setContentHash("new-hash");
        repository.save(doc);

        RawDocument found = repository.findBySourceId("src-1").orElseThrow();
        assertEquals("New Title", found.getTitle());
        assertEquals("New content", found.getContent());
        assertEquals("new-hash", found.getContentHash());
    }
}
