# Backend — LLM Wiki Platform

## OVERVIEW

Maven multi-module Java 17 + Spring Boot 3.2 backend. AI-powered knowledge base: syncs wiki sources, scores/extracts/vectorizes content, builds a knowledge graph, serves REST API.

## MODULES

Dependency chain (top to bottom):

```
llm-wiki-web → llm-wiki-service → llm-wiki-domain → llm-wiki-common
                                     llm-wiki-adapter → llm-wiki-common
```

- **llm-wiki-common** — Shared enums (`com.llmwiki.common.enums`), DTOs. Zero dependencies. Foundation layer.
- **llm-wiki-adapter** — AI API clients (`AiApiClient`, `EmbeddingClient`, `OpenAiApiClient`, `OpenAiEmbeddingClient` via WebFlux), wiki source adapters (`WikiSourceAdapter`). All behind interfaces — swap implementations freely.
- **llm-wiki-domain** — JPA entities + Spring Data repositories. Organized by domain: `sync/`, `graph/`, `page/`, `processing/`, `approval/`, `config/`.
- **llm-wiki-service** — Business logic: `pipeline/PipelineService`, `graph/KnowledgeGraphService`, `search/SearchService`, `approval/ApprovalService`, `sync/SyncService`.
- **llm-wiki-web** — REST controllers, `config/`, scheduler, entry point (`LlmWikiApplication`).

## WHERE TO LOOK

| What | Path |
|------|------|
| REST controllers | `llm-wiki-web/src/main/java/com/llmwiki/web/controller/` |
| Service classes | `llm-wiki-service/src/main/java/com/llmwiki/service/` |
| JPA entities / repos | `llm-wiki-domain/src/main/java/com/llmwiki/domain/` |
| AI adapter interfaces | `llm-wiki-adapter/src/main/java/com/llmwiki/adapter/api/` |
| Enums / DTOs | `llm-wiki-common/src/main/java/com/llmwiki/common/` |
| Flyway migrations | `llm-wiki-web/src/main/resources/db/migration/` |
| App config | `llm-wiki-web/src/main/resources/application.yml` |
| Spring Boot entry | `llm-wiki-web/src/main/java/com/llmwiki/web/LlmWikiApplication.java` |

## HIERARCHY

Each module has its own AGENTS.md:
- `llm-wiki-common/AGENTS.md` — Enums, DTOs, zero-dependency foundation
- `llm-wiki-adapter/AGENTS.md` — AI API clients, wiki adapters
- `llm-wiki-domain/AGENTS.md` — JPA entities, repositories
- `llm-wiki-service/AGENTS.md` — Business logic services
- `llm-wiki-web/AGENTS.md` — Controllers, config, scheduler

## CONVENTIONS

- **Package:** `com.llmwiki.{module}.{subdomain}` — module name matches the Maven artifact.
- **Entities:** Lombok (`@Getter @Setter @Builder`), UUID `@Id`, `@PrePersist`/`@PreUpdate` for auto timestamps.
- **Repositories:** Spring Data JPA interfaces in `com.llmwiki.domain.{domain}/repository/`.
- **Enums:** All in `llm-wiki-common/enums/`. Use `@Enumerated(EnumType.STRING)` on entity fields.
- **Adapter pattern:** AI clients implement interfaces in `adapter/api/`. Controller → Service → Adapter — never call adapters from controllers directly.
- **Flyway:** `V{version}__description.sql` in `db/migration/`. No `ddl-auto` changes.
- **Tests:** H2 in-memory DB. Main profile uses MariaDB 11.8+ with native VECTOR.

## ANTI-PATTERNS

- **No business logic in controllers.** Controllers marshal HTTP ↔ DTO. Delegate everything to services.
- **Don't bypass adapter interfaces.** Always use `AiApiClient`/`EmbeddingClient` for AI calls. Direct HTTP calls to AI APIs get rejected in review.
- **No deps on llm-wiki-common.** It's the foundation — adding dependencies would create circular chains.
- **`ddl-auto: update` is forbidden.** Schema is Flyway-managed (`validate` only). Changes go in migration SQL files.
- **Vectors only in `kg_vectors` table.** Use MariaDB `VECTOR(1536)` type. No inline vector columns.
- **No cross-module package scanning.** Spring Boot scans `com.llmwiki`. Each module's beans are picked up automatically.
