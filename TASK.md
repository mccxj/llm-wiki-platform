# Task: E-1 Source Grounding — Entity Position Alignment

## GitHub Issue
#33 [E-1][P0] Add source grounding — align entities to original text positions

## Problem
Current entity extraction returns only name/type/description with no connection to the original text. Cannot highlight entities in source, verify faithfulness, or provide traceability.

## LangExtract Reference Architecture
From LangExtract source code (google/langextract):
- `langextract/core/data.py`: `Extraction` class with `char_interval: CharInterval`, `alignment_status: AlignmentStatus`
- `langextract/core/tokenizer.py`: `Token`, `CharInterval(start_pos, end_pos)`, `TokenInterval`
- `langextract/resolver.py`: 3-tier alignment strategy:
  1. Exact match via `str.find()`
  2. Fuzzy match via LCS (Longest Common Subsequence) using `difflib.SequenceMatcher`
  3. Token-level alignment with density threshold (0.75 min, 1/3 density)
  - `AlignmentStatus`: MATCH_EXACT, MATCH_GREATER, MATCH_LESSER, MATCH_FUZZY

## Implementation Plan

### 1. Add common types (llm-wiki-common)
- `CharInterval` class: startOffset, endOffset
- `AlignmentStatus` enum: EXACT, FUZZY, GREATER, LESSER

### 2. Update ExtractionResult (llm-wiki-adapter)
Add to both `EntityInfo` and `ConceptInfo`:
- `startOffset` (Integer)
- `endOffset` (Integer)  
- `alignmentStatus` (AlignmentStatus)
- `extractionIndex` (Integer)

### 3. Update OpenAiApiClient (llm-wiki-adapter)
- Update entity extraction prompt to request character positions
- Update concept extraction prompt similarly
- Implement `alignEntity()` method (3-tier alignment)
- Implement `alignEntityBatch()` for post-processing

### 4. Implement AlignmentResolver (llm-wiki-adapter, new class)
- Port LangExtract's resolver logic to Java
- Exact match: `String.indexOf()`
- Fuzzy match: Java's equivalent of SequenceMatcher (use Apache Commons Text `SimilarityScore`)
- Token-level fallback with density check

### 5. Update PipelineService (llm-wiki-service)
- Pass grounding info through matchKnowledgeGraph()

### 6. Tests
- AlignmentResolverTest: exact, fuzzy, partial alignment scenarios
- OpenAiApiClientTest: verify position fields in parsed results
- PipelineServiceTest: verify grounding info flows through

## Files to Modify
- `backend/llm-wiki-common/src/main/java/com/llmwiki/common/types/CharInterval.java` (NEW)
- `backend/llm-wiki-common/src/main/java/com/llmwiki/common/enums/AlignmentStatus.java` (NEW)
- `backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/dto/ExtractionResult.java` (MODIFY)
- `backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/api/OpenAiApiClient.java` (MODIFY)
- `backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/resolver/AlignmentResolver.java` (NEW)
- `backend/llm-wiki-service/src/main/java/com/llmwiki/service/pipeline/PipelineService.java` (MODIFY)
- Tests for all new/modified classes

## Development Rules
- TDD: write tests first, then implement
- Follow existing code style and patterns
- All new classes need unit tests
- Run `mvn test` after each change to verify no regressions
