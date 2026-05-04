# llm-wiki-common — Common Module

**Module:** `com.llmwiki:llm-wiki-common`
**Depends on:** None (foundation layer)

## Overview

Shared enums, DTOs, and constants. Zero dependencies — every other module depends on this.

## Contents

| Package | Contents |
|---------|----------|
| `enums/` | `NodeType`, `EdgeType`, `PageStatus`, `ApprovalStatus`, `ApprovalAction`, `ScoringDimension`, `SyncStatus`, `PipelineStep`, `DocumentSource` |
| `dto/` | `SearchRequest`, `SearchResult`, `AnswerResult`, `ScoreResult` |

## Where to Look

| Task | Path |
|------|------|
| Enums | `src/main/java/com/llmwiki/common/enums/` |
| DTOs | `src/main/java/com/llmwiki/common/dto/` |
| Tests | `src/test/java/com/llmwiki/common/` |

## Anti-Patterns

- **Never add dependencies to this module** — it's the foundation
- Don't add business logic — only data definitions
- Don't create enums outside `enums/` package
- All entity enums must use `@Enumerated(EnumType.STRING)`
