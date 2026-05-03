package com.llmwiki.adapter.chunking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SlidingWindowChunkerTest {

    private SlidingWindowChunker chunker;

    @BeforeEach
    void setUp() {
        chunker = new SlidingWindowChunker(8000, 200);
    }

    // --- Basic chunking ---

    @Test
    void shouldReturnSingleChunkForShortText() {
        List<TextChunk> chunks = chunker.chunk("Hello world.");
        assertEquals(1, chunks.size());
        assertEquals("Hello world.", chunks.get(0).getText());
        assertEquals(0, chunks.get(0).getStartOffset());
        assertEquals(12, chunks.get(0).getEndOffset());
    }

    @Test
    void shouldReturnEmptyListForEmptyText() {
        List<TextChunk> chunks = chunker.chunk("");
        assertTrue(chunks.isEmpty());
    }

    @Test
    void shouldReturnEmptyListForBlankText() {
        List<TextChunk> chunks = chunker.chunk("   \n\n   ");
        assertTrue(chunks.isEmpty());
    }

    // --- Sentence boundary splitting ---

    @Test
    void shouldSplitOnSentenceBoundaries() {
        // Build text with clear sentences that exceed maxChunkSize when combined
        String sentence1 = "First sentence here. ";
        String sentence2 = "Second sentence follows. ";
        String sentence3 = "Third sentence is last. ";
        String text = sentence1 + sentence2 + sentence3;

        // Use a small max to force splitting
        SlidingWindowChunker smallChunker = new SlidingWindowChunker(
                sentence1.length() + sentence2.length() - 10, 0);
        List<TextChunk> chunks = smallChunker.chunk(text);
        assertTrue(chunks.size() >= 2, "Should split into multiple chunks");
    }

    @Test
    void shouldDetectSentenceBoundariesWithPeriods() {
        String text = "This is sentence one. This is sentence two. This is sentence three.";
        SlidingWindowChunker smallChunker = new SlidingWindowChunker(30, 0);
        List<TextChunk> chunks = smallChunker.chunk(text);
        assertTrue(chunks.size() >= 2);
    }

    @Test
    void shouldDetectSentenceBoundariesWithExclamationAndQuestion() {
        String text = "Is this working? Yes it is! That is great.";
        SlidingWindowChunker smallChunker = new SlidingWindowChunker(20, 0);
        List<TextChunk> chunks = smallChunker.chunk(text);
        assertTrue(chunks.size() >= 2);
    }

    // --- Overlap ---

    @Test
    void shouldHaveOverlapBetweenChunks() {
        // Create text long enough to require multiple chunks with overlap
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("Sentence number ").append(i).append(". ");
        }
        String text = sb.toString();

        SlidingWindowChunker overlapChunker = new SlidingWindowChunker(500, 100);
        List<TextChunk> chunks = overlapChunker.chunk(text);

        if (chunks.size() >= 2) {
            // Verify overlap markers are set on second chunk
            TextChunk second = chunks.get(1);
            assertTrue(second.getOverlapStart() > 0 || second.getOverlapEnd() > 0,
                    "Second chunk should have overlap region");
        }
    }

    @Test
    void overlapShouldNotExceedMaxChunkSize() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            sb.append("Sentence ").append(i).append(". ");
        }
        String text = sb.toString();

        SlidingWindowChunker overlapChunker = new SlidingWindowChunker(500, 100);
        List<TextChunk> chunks = overlapChunker.chunk(text);

        for (TextChunk chunk : chunks) {
            assertTrue(chunk.getText().length() <= 500,
                    "Chunk text length " + chunk.getText().length() + " exceeds max 500");
        }
    }

    // --- Position tracking ---

    @Test
    void shouldTrackStartOffsetCorrectly() {
        String text = "First sentence. Second sentence. Third sentence.";
        SlidingWindowChunker smallChunker = new SlidingWindowChunker(25, 0);
        List<TextChunk> chunks = smallChunker.chunk(text);

        assertEquals(0, chunks.get(0).getStartOffset(), "First chunk should start at 0");
        if (chunks.size() >= 2) {
            assertTrue(chunks.get(1).getStartOffset() > 0,
                    "Second chunk should have positive start offset");
        }
    }

    @Test
    void endOffsetShouldMatchTextLength() {
        String text = "Hello world.";
        List<TextChunk> chunks = chunker.chunk(text);
        assertEquals(text.length(), chunks.get(0).getEndOffset());
    }

    @Test
    void chunkTextShouldBeSubstringOfOriginal() {
        String text = "The quick brown fox jumps over the lazy dog. " +
                "A second sentence here. And a third one too.";
        SlidingWindowChunker smallChunker = new SlidingWindowChunker(40, 0);
        List<TextChunk> chunks = smallChunker.chunk(text);

        for (TextChunk chunk : chunks) {
            String substring = text.substring(chunk.getStartOffset(), chunk.getEndOffset());
            assertEquals(substring, chunk.getText(),
                    "Chunk text should match substring at tracked offsets");
        }
    }

    // --- Edge cases ---

    @Test
    void shouldHandleVeryLongSingleSentence() {
        // Single sentence longer than maxChunkSize
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("word").append(i).append(" ");
        }
        sb.append(".");
        String text = sb.toString();

        SlidingWindowChunker smallChunker = new SlidingWindowChunker(100, 10);
        List<TextChunk> chunks = smallChunker.chunk(text);

        assertFalse(chunks.isEmpty(), "Should produce at least one chunk");
        // All text should be covered
        int totalCovered = 0;
        for (TextChunk chunk : chunks) {
            totalCovered += chunk.getText().length();
        }
        assertTrue(totalCovered >= text.length(),
                "All text should be covered by chunks");
    }

    @Test
    void shouldHandleSingleCharacterText() {
        List<TextChunk> chunks = chunker.chunk("X");
        assertEquals(1, chunks.size());
        assertEquals("X", chunks.get(0).getText());
        assertEquals(0, chunks.get(0).getStartOffset());
        assertEquals(1, chunks.get(0).getEndOffset());
    }

    @Test
    void shouldHandleTextWithOnlyNewlines() {
        List<TextChunk> chunks = chunker.chunk("\n\n\n\n");
        assertTrue(chunks.isEmpty());
    }

    @Test
    void shouldHandleTextWithMultipleSpaces() {
        String text = "Word.    Word.    Word.";
        List<TextChunk> chunks = chunker.chunk(text);
        assertFalse(chunks.isEmpty());
    }

    // --- Default constructor ---

    @Test
    void defaultConstructorShouldUseDefaults() {
        SlidingWindowChunker defaultChunker = new SlidingWindowChunker();
        assertNotNull(defaultChunker);
        // Should handle normal text fine
        List<TextChunk> chunks = defaultChunker.chunk("Hello world.");
        assertEquals(1, chunks.size());
    }

    // --- Overlap region tracking ---

    @Test
    void firstChunkShouldHaveZeroOverlap() {
        List<TextChunk> chunks = chunker.chunk("Hello world.");
        assertEquals(0, chunks.get(0).getOverlapStart());
        assertEquals(0, chunks.get(0).getOverlapEnd());
    }

    @Test
    void overlapRegionShouldBeWithinChunk() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("Sentence ").append(i).append(". ");
        }
        String text = sb.toString();

        SlidingWindowChunker overlapChunker = new SlidingWindowChunker(300, 50);
        List<TextChunk> chunks = overlapChunker.chunk(text);

        for (int i = 1; i < chunks.size(); i++) {
            TextChunk chunk = chunks.get(i);
            if (chunk.getOverlapEnd() > 0) {
                assertTrue(chunk.getOverlapStart() >= chunk.getStartOffset(),
                        "Overlap start should be >= chunk start offset");
                assertTrue(chunk.getOverlapEnd() <= chunk.getEndOffset(),
                        "Overlap end should be <= chunk end offset");
            }
        }
    }

    // --- Chunk count ---

    @Test
    void shouldNotProduceMoreChunksThanNecessary() {
        // Short text should be single chunk
        String text = "Short text.";
        List<TextChunk> chunks = chunker.chunk(text);
        assertEquals(1, chunks.size());
    }

    @Test
    void chunksShouldCoverFullText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("This is sentence number ").append(i).append(". ");
        }
        String text = sb.toString();

        SlidingWindowChunker smallChunker = new SlidingWindowChunker(200, 50);
        List<TextChunk> chunks = smallChunker.chunk(text);

        // Verify coverage: first chunk starts at 0, last chunk ends at or near text length
        assertEquals(0, chunks.get(0).getStartOffset());
        // End offset should cover the text (may be slightly less due to trailing whitespace trim)
        int lastEnd = chunks.get(chunks.size() - 1).getEndOffset();
        assertTrue(lastEnd >= text.trim().length() && lastEnd <= text.length(),
                "Last chunk end offset " + lastEnd + " should be near text length " + text.length());
    }
}
