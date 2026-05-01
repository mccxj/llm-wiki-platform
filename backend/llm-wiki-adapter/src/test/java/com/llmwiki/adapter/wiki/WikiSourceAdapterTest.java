package com.llmwiki.adapter.wiki;

import com.llmwiki.adapter.dto.RawDocumentDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WikiSourceAdapterTest {

    MockWikiSourceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MockWikiSourceAdapter();
    }

    @Test
    void shouldReturnEmptyListWhenNoDocuments() {
        List<RawDocumentDTO> result = adapter.fetchChanges(Instant.EPOCH);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnAddedDocuments() {
        adapter.addDocument(new RawDocumentDTO("1", "Doc1", "# Content1", null, Instant.now()));
        adapter.addDocument(new RawDocumentDTO("2", "Doc2", "# Content2", null, Instant.now()));

        List<RawDocumentDTO> result = adapter.fetchChanges(Instant.EPOCH);
        assertEquals(2, result.size());
    }

    @Test
    void shouldFetchSinglePage() {
        adapter.addDocument(new RawDocumentDTO("1", "Doc1", "# Content1", "https://wiki.com/1", Instant.now()));

        RawDocumentDTO result = adapter.fetchPage("1");
        assertNotNull(result);
        assertEquals("Doc1", result.getTitle());
    }

    @Test
    void shouldReturnNullForMissingPage() {
        RawDocumentDTO result = adapter.fetchPage("nonexistent");
        assertNull(result);
    }

    @Test
    void shouldTestConnection() {
        assertTrue(adapter.testConnection());
        adapter.setAvailable(false);
        assertFalse(adapter.testConnection());
    }
}
