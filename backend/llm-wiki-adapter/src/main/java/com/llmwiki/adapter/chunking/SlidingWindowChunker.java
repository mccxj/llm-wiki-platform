package com.llmwiki.adapter.chunking;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sliding window text chunker that splits text on sentence boundaries
 * with configurable overlap between chunks.
 *
 * Inspired by langextract/chunking.py sliding window approach.
 * Sentence boundary detection is based on punctuation (. ! ?) followed
 * by whitespace or end of string.
 */
public class SlidingWindowChunker {

    private static final int DEFAULT_MAX_CHUNK_SIZE = 8000;
    private static final int DEFAULT_OVERLAP_SIZE = 200;

    // Matches sentence boundaries: . ! ? followed by whitespace or end of string
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("[.!?]+\\s+|[.!?]+$");

    private final int maxChunkSize;
    private final int overlapSize;

    /**
     * Creates a chunker with default maxChunkSize (8000) and overlapSize (200).
     */
    public SlidingWindowChunker() {
        this(DEFAULT_MAX_CHUNK_SIZE, DEFAULT_OVERLAP_SIZE);
    }

    /**
     * Creates a chunker with custom configuration.
     *
     * @param maxChunkSize maximum characters per chunk
     * @param overlapSize  number of overlapping characters between consecutive chunks
     */
    public SlidingWindowChunker(int maxChunkSize, int overlapSize) {
        if (maxChunkSize <= 0) throw new IllegalArgumentException("maxChunkSize must be positive");
        if (overlapSize < 0) throw new IllegalArgumentException("overlapSize must be non-negative");
        if (overlapSize >= maxChunkSize)
            throw new IllegalArgumentException("overlapSize must be less than maxChunkSize");
        this.maxChunkSize = maxChunkSize;
        this.overlapSize = overlapSize;
    }

    public int getMaxChunkSize() {
        return maxChunkSize;
    }

    public int getOverlapSize() {
        return overlapSize;
    }

    /**
     * Splits text into overlapping chunks at sentence boundaries.
     *
     * @param text the source text to chunk
     * @return list of TextChunk objects covering the entire text
     */
    public List<TextChunk> chunk(String text) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }

        // Trim leading/trailing whitespace but track original offsets
        int leadingTrim = 0;
        while (leadingTrim < text.length() && Character.isWhitespace(text.charAt(leadingTrim))) {
            leadingTrim++;
        }
        int trailingTrim = text.length();
        while (trailingTrim > leadingTrim && Character.isWhitespace(text.charAt(trailingTrim - 1))) {
            trailingTrim--;
        }

        if (leadingTrim >= trailingTrim) {
            return new ArrayList<>();
        }

        String trimmed = text.substring(leadingTrim, trailingTrim);

        // If entire text fits in one chunk, return it
        if (trimmed.length() <= maxChunkSize) {
            return List.of(new TextChunk(trimmed, leadingTrim, trailingTrim, 0, 0));
        }

        // Find all sentences with their positions in trimmed text
        List<Sentence> sentences = splitIntoSentences(trimmed);

        List<TextChunk> chunks = new ArrayList<>();
        int chunkStartInTrimmed = 0;  // Start of current chunk in trimmed text
        int posInTrimmed = 0;         // Current position while building chunk

        int chunkSentenceStart = 0;   // Index of first sentence in current chunk

        for (int i = 0; i < sentences.size(); i++) {
            Sentence sentence = sentences.get(i);
            int sentenceEnd = sentence.end();

            // Check if adding this sentence would exceed maxChunkSize
            int chunkLen = sentenceEnd - chunkStartInTrimmed;

            if (chunkLen > maxChunkSize) {
                // Need to emit a chunk before this sentence
                if (posInTrimmed > chunkStartInTrimmed) {
                    // Emit chunk up to (but not including) current sentence
                    int chunkEndInTrimmed = posInTrimmed;
                    int overlapStart = 0;
                    int overlapEnd = 0;

                    // Set overlap markers: the overlap region is the end of this chunk
                    // that will be re-sent at the start of the next chunk
                    if (overlapSize > 0 && chunkEndInTrimmed > chunkStartInTrimmed) {
                        int overlapRegionStart = Math.max(chunkStartInTrimmed,
                                chunkEndInTrimmed - overlapSize);
                        overlapStart = overlapRegionStart + leadingTrim;
                        overlapEnd = chunkEndInTrimmed + leadingTrim;
                    }

                    String chunkText = trimmed.substring(chunkStartInTrimmed, chunkEndInTrimmed);
                    chunks.add(new TextChunk(chunkText,
                            chunkStartInTrimmed + leadingTrim,
                            chunkEndInTrimmed + leadingTrim,
                            overlapStart, overlapEnd));

                    // Next chunk starts overlapSize chars before the end of this chunk
                    chunkStartInTrimmed = Math.max(chunkStartInTrimmed,
                            chunkEndInTrimmed - overlapSize);

                    // Find the sentence index where the new chunk starts
                    chunkSentenceStart = findSentenceIndexAtOffset(sentences, chunkStartInTrimmed);
                    i = chunkSentenceStart - 1; // Will be incremented by loop
                    posInTrimmed = chunkStartInTrimmed;
                    continue;
                } else {
                    // Single sentence is longer than maxChunkSize: force-split
                    String longSentence = trimmed.substring(chunkStartInTrimmed, sentenceEnd);
                    chunks.add(new TextChunk(longSentence,
                            chunkStartInTrimmed + leadingTrim,
                            sentenceEnd + leadingTrim, 0, 0));

                    // Apply overlap for next chunk
                    if (overlapSize > 0 && overlapSize < longSentence.length()) {
                        chunkStartInTrimmed = sentenceEnd - overlapSize;
                    } else {
                        chunkStartInTrimmed = sentenceEnd;
                    }

                    chunkSentenceStart = i + 1;
                    posInTrimmed = chunkStartInTrimmed;
                }
            } else {
                posInTrimmed = sentenceEnd;
            }
        }

        // Emit remaining text
        if (chunkStartInTrimmed < trimmed.length()) {
            int overlapStart = 0;
            int overlapEnd = 0;
            if (overlapSize > 0 && chunkStartInTrimmed > 0) {
                int overlapRegionStart = Math.max(0, chunkStartInTrimmed);
                overlapStart = overlapRegionStart + leadingTrim;
                overlapEnd = (chunkStartInTrimmed + overlapSize < trimmed.length())
                        ? chunkStartInTrimmed + overlapSize + leadingTrim
                        : trimmed.length() + leadingTrim;
            }

            String chunkText = trimmed.substring(chunkStartInTrimmed);
            chunks.add(new TextChunk(chunkText,
                    chunkStartInTrimmed + leadingTrim,
                    trailingTrim,
                    overlapStart, overlapEnd));
        }

        return chunks;
    }

    /**
     * Splits text into sentences using regex-based boundary detection.
     */
    private List<Sentence> splitIntoSentences(String text) {
        List<Sentence> sentences = new ArrayList<>();
        if (text.isEmpty()) return sentences;

        Matcher matcher = SENTENCE_BOUNDARY.matcher(text);
        int sentenceStart = 0;

        while (matcher.find()) {
            int end = matcher.end();
            // Trim trailing whitespace from sentence
            int contentEnd = end;
            while (contentEnd > sentenceStart && Character.isWhitespace(text.charAt(contentEnd - 1))) {
                contentEnd--;
            }
            if (contentEnd > sentenceStart) {
                sentences.add(new Sentence(sentenceStart, end));
            }
            sentenceStart = end;
        }

        // Handle trailing text without sentence-ending punctuation
        if (sentenceStart < text.length()) {
            String trailing = text.substring(sentenceStart).trim();
            if (!trailing.isEmpty()) {
                sentences.add(new Sentence(sentenceStart, text.length()));
            }
        }

        return sentences;
    }

    /**
     * Finds the sentence index that contains or is at the given offset.
     */
    private int findSentenceIndexAtOffset(List<Sentence> sentences, int offset) {
        for (int i = 0; i < sentences.size(); i++) {
            if (sentences.get(i).start() >= offset) {
                return i;
            }
        }
        return sentences.size();
    }

    /**
     * Represents a sentence with its start and end positions in the source text.
     */
    private record Sentence(int start, int end) {
    }
}
