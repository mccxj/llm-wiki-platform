# Task: E-3 Multi-Pass Extraction

## GitHub Issue
#35 [E-3][P1] Add multi-pass extraction for improved recall on long documents

## Problem
Single-pass extraction misses entities in long documents. LangExtract uses extraction_passes=3 for 10-20% recall improvement.

## Implementation Plan

### 1. Add config
- pipeline.extraction.passes (int, default 1)
- pipeline.extraction.temperatures (comma-separated, default "0.1,0.3,0.5")

### 2. Update OpenAiApiClient
- extractEntitiesMultiPass(content, passes): make N calls with different temperatures
- extractConceptsMultiPass(content, passes): same for concepts
- Merge results across passes: entities in 2+ passes get confidence boost

### 3. Update PipelineService
- Call multi-pass methods when passes > 1
- Track extractionPass per entity

### 4. Tests
- Multi-pass merge logic
- Config parsing
- Temperature variation

## Files to Modify
- backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/api/OpenAiApiClient.java
- backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/dto/ExtractionResult.java (add confidence field)
