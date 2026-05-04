package com.llmwiki.adapter.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class RawDocumentDTOTest {

    @Test
    void shouldCreateWithNoArgsConstructor() {
        RawDocumentDTO dto = new RawDocumentDTO();
        assertNull(dto.getSourceId());
        assertNull(dto.getTitle());
        assertNull(dto.getContent());
        assertNull(dto.getSourceUrl());
        assertNull(dto.getLastModified());
        assertNull(dto.getFormat());
    }

    @Test
    void shouldCreateWithAllArgsConstructor() {
        Instant now = Instant.now();
        RawDocumentDTO dto = new RawDocumentDTO("src-1", "Test Title", "Test Content", "https://wiki.com/1", now);

        assertEquals("src-1", dto.getSourceId());
        assertEquals("Test Title", dto.getTitle());
        assertEquals("Test Content", dto.getContent());
        assertEquals("https://wiki.com/1", dto.getSourceUrl());
        assertEquals(now, dto.getLastModified());
    }

    @Test
    void shouldSetAndGetAllFields() {
        RawDocumentDTO dto = new RawDocumentDTO();
        Instant now = Instant.now();

        dto.setSourceId("src-2");
        dto.setTitle("Another Title");
        dto.setContent("More content");
        dto.setSourceUrl("https://wiki.com/2");
        dto.setLastModified(now);
        dto.setFormat("markdown");

        assertEquals("src-2", dto.getSourceId());
        assertEquals("Another Title", dto.getTitle());
        assertEquals("More content", dto.getContent());
        assertEquals("https://wiki.com/2", dto.getSourceUrl());
        assertEquals(now, dto.getLastModified());
        assertEquals("markdown", dto.getFormat());
    }

    @Test
    void shouldHandleNullValues() {
        RawDocumentDTO dto = new RawDocumentDTO(null, null, null, null, null);

        assertNull(dto.getSourceId());
        assertNull(dto.getTitle());
        assertNull(dto.getContent());
        assertNull(dto.getSourceUrl());
        assertNull(dto.getLastModified());
    }

    @Test
    void shouldHandleEmptyStrings() {
        RawDocumentDTO dto = new RawDocumentDTO("", "", "", "", Instant.EPOCH);

        assertEquals("", dto.getSourceId());
        assertEquals("", dto.getTitle());
        assertEquals("", dto.getContent());
        assertEquals("", dto.getSourceUrl());
    }

    @Test
    void formatShouldDefaultToNull() {
        RawDocumentDTO dto = new RawDocumentDTO("src", "title", "content", "url", Instant.now());
        assertNull(dto.getFormat());
    }
}
