# llm-wiki-adapter — Adapter Layer

**Module:** `com.llmwiki:llm-wiki-adapter`
**Depends on:** `llm-wiki-common`

## Overview

External service adapters: AI API clients (OpenAI-compatible) and wiki source adapters. All behind interfaces for swappable implementations.

## Interfaces

| Interface | Implementation | Purpose |
|-----------|---------------|---------|
| `AiApiClient` | `OpenAiApiClient` | Chat completions for scoring, entity/concept extraction |
| `EmbeddingClient` | `OpenAiEmbeddingClient` | Text embedding via OpenAI-compatible API |
| `WikiSourceAdapter` | (multiple) | Fetch documents from wiki sources |

## Where to Look

| Task | Path |
|------|------|
| Interface definitions | `src/main/java/com/llmwiki/adapter/api/` |
| Implementations | `src/main/java/com/llmwiki/adapter/api/impl/` or same package |
| Wiki adapters | `src/main/java/com/llmwiki/adapter/wiki/` |
| Tests | `src/test/java/com/llmwiki/adapter/` |

## Conventions

- All AI calls go through `AiApiClient`/`EmbeddingClient` interfaces
- WebFlux (`WebClient`) for non-blocking HTTP to AI API
- Responses are parsed from JSON; handle malformed responses gracefully
- `AdapterFactory` selects the right `WikiSourceAdapter` by source type

## Anti-Patterns

- Don't add Spring MVC dependencies — this module is web-framework-agnostic
- Don't call domain/service classes from adapters
- Don't hardcode API URLs — use config from `application.yml`
