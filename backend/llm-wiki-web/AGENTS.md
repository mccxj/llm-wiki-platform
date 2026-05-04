# llm-wiki-web — Web Layer

**Module:** `com.llmwiki:llm-wiki-web`
**Depends on:** `llm-wiki-service`

## Overview

REST controllers, Spring Boot entry point (`LlmWikiApplication`), configuration, scheduled tasks, and security (JWT).

## Controllers

| Controller | Base Path | Endpoints |
|------------|-----------|-----------|
| `PageController` | `/api/pages` | list, getById, links, history, delete |
| `PipelineController` | `/api/pipeline` | process, logs, pending, pages, page, deadLetters, retryDeadLetter |
| `SearchController` | `/api/search` | search, ask, searchByTag, searchByRelation |
| `ApprovalController` | `/api/approvals` | submit, approve, reject, pending, list, getById, batchApprove, batchReject, history |
| `AuthController` | `/api/auth` | login, register, verify |
| `EntityExampleController` | `/api/examples` | list, listByType, create, update, delete, getById |

## Configuration

| File | Purpose |
|------|---------|
| `application.yml` | Main config: DB, AI API, Flyway, scheduler |
| `db/migration/` | Flyway SQL migrations (`V{version}__description.sql`) |
| `config/` | Spring `@Configuration` beans |

## Security

- JWT-based auth via `JwtTokenProvider`
- `JwtAuthenticationFilter` intercepts requests
- `AuthController` is unauthenticated; all others require valid JWT

## Scheduler

- `SyncScheduler` polls wiki sources on a fixed schedule (configurable via `application.yml`)

## Where to Look

| Task | Path |
|------|------|
| Controllers | `src/main/java/com/llmwiki/web/controller/` |
| Config | `src/main/java/com/llmwiki/web/config/` |
| Security | `src/main/java/com/llmwiki/web/security/` |
| Scheduler | `src/main/java/com/llmwiki/web/scheduler/` |
| Entry point | `src/main/java/com/llmwiki/web/LlmWikiApplication.java` |
| Tests | `src/test/java/com/llmwiki/web/controller/` |

## Anti-Patterns

- Controllers must NOT contain business logic — delegate to services
- Don't bypass the service layer for direct repository access
- Don't hardcode config values — use `application.yml` env vars
