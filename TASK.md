# Task: E-2 Few-Shot Prompting Support

## GitHub Issue
#34 [E-2][P0] Add few-shot prompting support for entity/concept extraction

## Problem
Current extraction uses zero-shot prompting which produces inconsistent quality for domain-specific content. LangExtract uses few-shot examples to improve accuracy by 15-30%.

## LangExtract Reference Architecture
From LangExtract source code (google/langextract):
- `langextract/core/data.py`: `ExampleData` dataclass with `text` + `extractions` list
- `langextract/prompting.py`: `PromptTemplateStructured` with `description` + `examples`
- `langextract/prompting.py`: `QAPromptGenerator` renders examples into prompt text

Key pattern from LangExtract:
```
prompt = description + "\n\n" + formatted_examples + "\n\n" + actual_input

Where formatted_examples looks like:
"Example 1:
Text: {example_text}
Extractions: {example_extractions}

Example 2:
Text: {example_text}
Extractions: {example_extractions}"
```

## Implementation Plan

### 1. Add ExampleData class (llm-wiki-adapter)
```java
public class ExampleData {
    private String text;
    private List<LabeledExtraction> extractions;
    
    public static class LabeledExtraction {
        private String extractionClass;  // entity class like "PERSON", "ORG"
        private String extractionText;   // the actual text
        private String description;
        private List<String> attributes; // optional
    }
}
```

### 2. Add PromptTemplate class (llm-wiki-adapter)
```java
public class PromptTemplate {
    private String description;
    private List<ExampleData> examples;
    
    public String render(String inputText) {
        // Build prompt: description + examples + input
    }
}
```

### 3. Create EntityExample entity + repository (llm-wiki-domain)
- `EntityExample` JPA entity: id, name, exampleType (ENTITY|CONCEPT), text, extractions (JSON), createdAt
- `EntityExampleRepository`: findByType(), findAllActive()

### 4. Update OpenAiApiClient (llm-wiki-adapter)
- Add `List<ExampleData>` parameter to extractEntities() and extractConcepts()
- Build few-shot prompt when examples are available
- Fall back to zero-shot when no examples

### 5. Create EntityExampleService (llm-wiki-service)
- CRUD for example management
- Load examples from DB, convert to ExampleData

### 6. DB Migration
- V8__create_entity_examples.sql

### 7. REST API (llm-wiki-web)
- GET /api/examples — list all examples
- POST /api/examples — create example
- PUT /api/examples/{id} — update example
- DELETE /api/examples/{id} — delete example

### 8. Tests
- PromptTemplateTest: verify prompt rendering
- EntityExampleServiceTest: CRUD operations
- OpenAiApiClientTest: verify few-shot prompt structure when examples provided
- EntityExampleRepositoryTest: DB queries

## Files to Create/Modify
NEW:
- backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/dto/ExampleData.java
- backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/prompting/PromptTemplate.java
- backend/llm-wiki-domain/src/main/java/com/llmwiki/domain/example/entity/EntityExample.java
- backend/llm-wiki-domain/src/main/java/com/llmwiki/domain/example/repository/EntityExampleRepository.java
- backend/llm-wiki-service/src/main/java/com/llmwiki/service/example/EntityExampleService.java
- backend/llm-wiki-web/src/main/java/com/llmwiki/web/controller/EntityExampleController.java
- backend/llm-wiki-web/src/main/resources/db/migration/V8__create_entity_examples.sql

MODIFY:
- backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/api/AiApiClient.java (add examples param)
- backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/api/OpenAiApiClient.java (implement few-shot)
- backend/llm-wiki-service/src/main/java/com/llmwiki/service/pipeline/PipelineService.java (pass examples)

## Development Rules
- TDD: write tests first, then implement
- Follow existing code style and patterns
- All new classes need unit tests
- Run `mvn test` after each change to verify no regressions
