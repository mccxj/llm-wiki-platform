package com.llmwiki.adapter.chunking;

import java.util.Objects;

/**
 * Represents a chunk of text extracted from a source document,
 * with position tracking and overlap region information.
 *
 * Inspired by langextract/chunking.py TextChunk with token_interval.
 */
public class TextChunk {

    private final String text;
    private final int startOffset;
    private final int endOffset;
    private final int overlapStart;
    private final int overlapEnd;

    /**
     * Creates a text chunk.
     *
     * @param text        the chunk text content
     * @param startOffset character position where this chunk starts in the source document
     * @param endOffset   character position where this chunk ends in the source document
     * @param overlapStart character position where the overlap region starts (0 if no overlap)
     * @param overlapEnd   character position where the overlap region ends (0 if no overlap)
     */
    public TextChunk(String text, int startOffset, int endOffset, int overlapStart, int overlapEnd) {
        this.text = text;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.overlapStart = overlapStart;
        this.overlapEnd = overlapEnd;
    }

    public String getText() {
        return text;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public int getOverlapStart() {
        return overlapStart;
    }

    public int getOverlapEnd() {
        return overlapEnd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TextChunk that = (TextChunk) o;
        return startOffset == that.startOffset &&
                endOffset == that.endOffset &&
                overlapStart == that.overlapStart &&
                overlapEnd == that.overlapEnd &&
                Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, startOffset, endOffset, overlapStart, overlapEnd);
    }

    @Override
    public String toString() {
        return "TextChunk{" +
                "text='" + text + '\'' +
                ", startOffset=" + startOffset +
                ", endOffset=" + endOffset +
                ", overlapStart=" + overlapStart +
                ", overlapEnd=" + overlapEnd +
                '}';
    }
}
