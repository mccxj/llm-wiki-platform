package com.llmwiki.adapter.chunking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextChunkTest {

    @Test
    void shouldCreateTextChunkWithAllFields() {
        TextChunk chunk = new TextChunk("Hello world.", 0, 12, 0, 0);
        assertEquals("Hello world.", chunk.getText());
        assertEquals(0, chunk.getStartOffset());
        assertEquals(12, chunk.getEndOffset());
        assertEquals(0, chunk.getOverlapStart());
        assertEquals(0, chunk.getOverlapEnd());
    }

    @Test
    void shouldCreateTextChunkWithOverlap() {
        TextChunk chunk = new TextChunk("overlapping text here", 100, 121, 100, 115);
        assertEquals("overlapping text here", chunk.getText());
        assertEquals(100, chunk.getStartOffset());
        assertEquals(121, chunk.getEndOffset());
        assertEquals(100, chunk.getOverlapStart());
        assertEquals(115, chunk.getOverlapEnd());
    }

    @Test
    void shouldBeEqualWithSameFields() {
        TextChunk chunk1 = new TextChunk("text", 0, 4, 0, 0);
        TextChunk chunk2 = new TextChunk("text", 0, 4, 0, 0);
        assertEquals(chunk1, chunk2);
        assertEquals(chunk1.hashCode(), chunk2.hashCode());
    }

    @Test
    void shouldNotBeEqualWithDifferentFields() {
        TextChunk chunk1 = new TextChunk("text", 0, 4, 0, 0);
        TextChunk chunk2 = new TextChunk("other", 0, 5, 0, 0);
        assertNotEquals(chunk1, chunk2);
    }

    @Test
    void toStringShouldContainAllFields() {
        TextChunk chunk = new TextChunk("hello", 0, 5, 0, 0);
        String str = chunk.toString();
        assertTrue(str.contains("hello"));
        assertTrue(str.contains("0"));
        assertTrue(str.contains("5"));
    }
}
