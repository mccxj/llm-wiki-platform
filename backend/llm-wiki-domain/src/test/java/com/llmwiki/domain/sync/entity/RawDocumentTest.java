package com.llmwiki.domain.sync.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RawDocumentTest {

    @Test
    void shouldCreateWithBuilder() {
        RawDocument doc = RawDocument.builder()
                .id(UUID.randomUUID())
                .sourceId("src-1")
                .sourceName("test-wiki")
                .title("Test Document")
                .content("Test content")
                .contentHash("abc123")
                .build();

        assertNotNull(doc.getId());
        assertEquals("src-1", doc.getSourceId());
        assertEquals("test-wiki", doc.getSourceName());
        assertEquals("Test Document", doc.getTitle());
        assertEquals("Test content", doc.getContent());
        assertEquals("abc123", doc.getContentHash());
    }

    @Test
    void shouldHandleNullFields() {
        RawDocument doc = RawDocument.builder()
                .sourceId("src-1")
                .sourceName("test")
                .title("Test")
                .content("Content")
                .contentHash("hash")
                .build();

        assertNotNull(doc);
        assertNull(doc.getId());
        assertNull(doc.getSourceUrl());
    }
}
