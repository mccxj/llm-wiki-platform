# LLM Wiki Platform — Knowledge Base

**Generated:** 2026-05-04
**Stack:** Java 17 + Spring Boot 3.2 + React 18 + TypeScript + MariaDB 11.8+ (native VECTOR)

## Overview

AI-powered knowledge base automation platform. Syncs documents from wiki sources, processes them through an AI pipeline (scoring → entity extraction → vectorization), builds a knowledge graph, and serves via REST API + React frontend.

## Structure

```
llm-wiki-platform/
├── backend/                  # Maven multi-module Java backend
│   ├── llm-wiki-common/     # Shared enums, DTOs (no deps)
│   ├── llm-wiki-adapter/    # AI API clients (OpenAI-compatible), wiki source adapters
│   ├── llm-wiki-domain/     # JPA entities, Spring Data repositories
│   ├── llm-wiki-service/    # Business logic: pipeline, sync, search, graph, approval
│   └── llm-wiki-web/        # Controllers, config, scheduler, entry point
├── frontend/                 # React 18 + Vite + Ant Design + D3.js
│   ├── src/pages/           # Dashboard, Pages, Search, Approvals, Graph
│   ├── src/components/      # Layout
│   └── src/api.ts           # Axios API client
├── docker-compose.yml        # mariadb + backend + frontend
├── Dockerfile               # Multi-stage: Maven build → JRE 17 runtime
└── .github/workflows/       # OpenCode AI agent CI
```

## HIERARCHY

- `AGENTS.md` (this file) — Project overview, architecture, test coverage
- `backend/AGENTS.md` — Backend module summary + hierarchy
- `backend/llm-wiki-{common,adapter,domain,service,web}/AGENTS.md` — Per-module details
- `frontend/AGENTS.md` — Frontend structure and conventions

## Where to Look

| Task | Location |
|------|----------|
| Add API endpoint | `backend/llm-wiki-web/src/main/java/com/llmwiki/web/controller/` |
| Add business logic | `backend/llm-wiki-service/src/main/java/com/llmwiki/service/` |
| Add entity/repository | `backend/llm-wiki-domain/src/main/java/com/llmwiki/domain/` |
| Add AI client | `backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/api/` |
| Add enum/constants | `backend/llm-wiki-common/src/main/java/com/llmwiki/common/enums/` |
| Add frontend page | `frontend/src/pages/` |
| DB schema changes | `backend/llm-wiki-web/src/main/resources/db/migration/` |
| App config | `backend/llm-wiki-web/src/main/resources/application.yml` |

## Architecture

**Dependency chain (Maven modules):**
```
llm-wiki-web → llm-wiki-service → llm-wiki-domain → llm-wiki-common
                                     llm-wiki-adapter → llm-wiki-common
```

**Processing pipeline:**
1. `SyncScheduler` polls wiki sources → `RawDocument`
2. `PipelineService` → AI scoring → entity/concept extraction → vector embedding
3. `KnowledgeGraphService` → creates `KgNode`/`KgEdge` + `KgVector`
4. Pages generated → approval queue → published

## Conventions

- **Java package:** `com.llmwiki.{module}.{layer}`
- **Entities:** JPA with Lombok (`@Getter @Setter @Builder`), UUID `@Id`, `@PrePersist`/`@PreUpdate` for timestamps
- **Repositories:** Spring Data JPA interfaces in `domain/**/repository/`
- **Enums:** All in `llm-wiki-common/enums/`, use `@Enumerated(EnumType.STRING)`
- **DB migrations:** Flyway, `V{version}__description.sql` format
- **Frontend:** Functional components + hooks, Ant Design for UI, D3.js for graph viz
- **API client:** Single `api.ts` with Axios, proxied via Vite to `localhost:8080`

## Anti-Patterns

- Don't add business logic to `llm-wiki-web` controllers — delegate to service layer
- Don't bypass the adapter interface for AI calls — use `AiApiClient`/`EmbeddingClient` interfaces
- Don't add dependencies to `llm-wiki-common` — it's the foundation module
- Don't use `ddl-auto: update` — schema is Flyway-managed (`validate` only)
- Don't store vectors outside `kg_vectors` table — use MariaDB `VECTOR(1536)` type

## MariaDB VECTOR Conventions

- **All VECTOR columns** must use `@Type(MariaDBVectorType.class)` with `@Column(columnDefinition = "VECTOR(1536)")`
- **Do NOT use `@Convert` with a JSON serializer** for VECTOR columns — the JDBC driver requires `setObject(float[])`, not `setString()`, to write to MariaDB VECTOR columns
- **Native SQL vector queries** use `VEC_DISTANCE()` and `VEC_FromText()` MariaDB functions (see `SearchService`, `SemanticDedupService`)
- **MariaDB Connector/J** must be ≥ 3.5.0 for native VECTOR support (currently 3.5.2)

## Commands

```bash
# Backend
cd backend && mvn clean package -DskipTests    # build
cd backend && mvn spring-boot:run -pl llm-wiki-web  # run locally

# Frontend
cd frontend && npm install && npm run dev      # dev server :3000
cd frontend && npm run build                   # production build

# Docker
docker-compose up -d                           # start all services
docker-compose logs -f backend                 # tail backend logs

# DB migration (Flyway)
# Just add V{N}__description.sql to src/main/resources/db/migration/
```

## Config

All externalized via `application.yml` env vars:
- `DB_HOST`, `DB_USERNAME`, `DB_PASSWORD` — MariaDB
- `AI_API_BASE_URL`, `AI_API_KEY`, `AI_MODEL` — OpenAI-compatible API
- `EMBEDDING_MODEL`, `EMBEDDING_DIMENSION` — vector config (default: ada-002, 1536)

## Test Coverage

**Total: 556 tests across 5 modules (all passing, 4 skipped require Docker)**

| Module | Tests | Coverage Focus |
|--------|-------|---------------|
| `llm-wiki-common` | 48 | Enums, DTOs, scoring logic |
| `llm-wiki-adapter` | 111 | AI API clients, embedding, wiki adapters |
| `llm-wiki-domain` | 106 | JPA entities, repository queries, vector UserType |
| `llm-wiki-service` | 172 | Pipeline, sync, search, graph, approval, maintenance |
| `llm-wiki-web` | 119 | REST controllers, request/response handling (4 skipped: Docker-only) |

**Test conventions:**
- `@ExtendWith(MockitoExtension.class)` for unit tests
- `@Mock` / `@InjectMocks` for dependency injection
- H2 in-memory DB for repository tests
- `PageControllerTest` covers all 6 endpoints (list, getById, links, history, delete, 404)
- `KnowledgeGraphServiceTest` covers BFS shortest path, graph stats, orphan counting
- `ApprovalControllerTest` covers batch approve/reject, partial failures, history
- `MaintenanceServiceTest` covers stale pages, contested pages, index consistency
- `SearchServiceTest` covers offset pagination, dedup

## Notes

- Frontend locale is `zh_CN` (Chinese)
- `code-review-graph` MCP is installed — use `detect_changes`, `query_graph`, `get_impact_radius` before manual grep for code review
- Spring Boot scans `com.llmwiki` base package; JPA repositories and entities are in `com.llmwiki.domain`
- Tests use H2 in-memory DB; main profile uses MariaDB 11.8+ with native VECTOR

<!-- code-review-graph MCP tools -->
## MCP Tools: code-review-graph

**IMPORTANT: This project has a knowledge graph. ALWAYS use the
code-review-graph MCP tools BEFORE using Grep/Glob/Read to explore
the codebase.** The graph is faster, cheaper (fewer tokens), and gives
you structural context (callers, dependents, test coverage) that file
scanning cannot.

### When to use graph tools FIRST

- **Exploring code**: `semantic_search_nodes` or `query_graph` instead of Grep
- **Understanding impact**: `get_impact_radius` instead of manually tracing imports
- **Code review**: `detect_changes` + `get_review_context` instead of reading entire files
- **Finding relationships**: `query_graph` with callers_of/callees_of/imports_of/tests_for
- **Architecture questions**: `get_architecture_overview` + `list_communities`

Fall back to Grep/Glob/Read **only** when the graph doesn't cover what you need.

### Key Tools

| Tool | Use when |
|------|----------|
| `detect_changes` | Reviewing code changes — gives risk-scored analysis |
| `get_review_context` | Need source snippets for review — token-efficient |
| `get_impact_radius` | Understanding blast radius of a change |
| `get_affected_flows` | Finding which execution paths are impacted |
| `query_graph` | Tracing callers, callees, imports, tests, dependencies |
| `semantic_search_nodes` | Finding functions/classes by name or keyword |
| `get_architecture_overview` | Understanding high-level codebase structure |
| `refactor_tool` | Planning renames, finding dead code |

### Workflow

1. The graph auto-updates on file changes (via hooks).
2. Use `detect_changes` for code review.
3. Use `get_affected_flows` to understand impact.
4. Use `query_graph` pattern="tests_for" to check coverage.
