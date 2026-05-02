# Task: E-4 Sliding Window Chunking

## GitHub Issue
#36 [E-4][P1] Replace naive chunking with sliding window strategy

## Problem
Current chunking splits on \n\n with no overlap. Entities at boundaries are truncated.

## LangExtract Reference
- langextract/chunking.py: TextChunk with token_interval tracking position in source
- langextract/core/tokenizer.py: Sentence boundary detection

## Implementation Plan

### 1. Create TextChunk class (llm-wiki-adapter)
```java
public class TextChunk {
    private String text;
    private int startOffset;  // char position in source
    private int endOffset;
    private int overlapStart; // overlap region start
    private int overlapEnd;   // overlap region end
}
```

### 2. Create SlidingWindowChunker (llm-wiki-adapter, new class)
- Config: maxChunkSize (default 8000), overlapSize (default 200)
- Split on sentence boundaries when possible
- Track position in source document
- Handle edge cases: single sentence > maxChunkSize

### 3. Update OpenAiApiClient
- Replace splitIntoChunks() with SlidingWindowChunker
- Track chunk positions for entity offset adjustment

### 4. Tests
- Chunking with overlap
- Sentence boundary detection
- Position tracking accuracy
- Edge cases: very long sentence, empty text

## Files to Create
- backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/chunking/TextChunk.java
- backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/chunking/SlidingWindowChunker.java

## Files to Modify
- backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/api/OpenAiApiClient.java
