# Task: E-5 Unified Extraction

## GitHub Issue
#37 [E-5][P1] Unified extraction — single LLM call for entities + concepts + relations

## Problem
Two separate LLM calls for entities/concepts causes inconsistent classification and 2x API cost.

## Implementation Plan

### 1. Add extractAll() to AiApiClient interface
```java
ExtractionResult extractAll(String content);
```

### 2. Update ExtractionResult
- Add relations field: List<RelationInfo>
- RelationInfo: sourceName, targetName, type, confidence

### 3. Update OpenAiApiClient
- Unified prompt requesting entities + concepts + relations in one JSON call
- New response schema:
```json
{
  "entities": [...],
  "concepts": [...],
  "relations": [{"source": "Java", "target": "OOP", "type": "implements"}]
}
```

### 4. Update PipelineService
- Use extractAll() instead of separate extractEntities() + extractConcepts()
- Process relations from unified result

### 5. Tests
- Unified prompt structure
- Relation parsing
- Backward compatibility with separate calls

## Files to Modify
- backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/api/AiApiClient.java
- backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/dto/ExtractionResult.java
- backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/api/OpenAiApiClient.java
- backend/llm-wiki-service/src/main/java/com/llmwiki/service/pipeline/PipelineService.java
