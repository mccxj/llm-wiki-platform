# llm-wiki-domain — Domain Layer

**Module:** `com.llmwiki:llm-wiki-domain`
**Depends on:** `llm-wiki-common`

## Overview

JPA entities and Spring Data repositories. Organized by domain area under `com.llmwiki.domain`.

## Domain Areas

| Area | Entities | Purpose |
|------|----------|---------|
| `sync` | `RawDocument`, `WikiSource`, `SyncLog` | Wiki source polling + raw document ingestion |
| `graph` | `KgNode`, `KgEdge`, `KgVector` | Knowledge graph with pgvector embeddings |
| `page` | `Page`, `PageLink`, `PageTag`, `PageTagId` | Generated pages + cross-references + tags |
| `processing` | `ProcessingLog` | Pipeline step tracking per document |
| `approval` | `ApprovalQueue` | Approval workflow for generated pages |
| `config` | `SystemConfig` | Key-value system settings (scoring thresholds, etc.) |

## Where to Look

| Task | Path |
|------|------|
| Entity definitions | `src/main/java/com/llmwiki/domain/{area}/entity/` |
| Repositories | `src/main/java/com/llmwiki/domain/{area}/repository/` |

## Conventions

- Entities: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`
- ID: `@Id @GeneratedValue(strategy = GenerationType.UUID)` → `UUID` type
- Timestamps: `@PrePersist`/`@PreUpdate` with `Instant.now()`
- Enums: `@Enumerated(EnumType.STRING)`, defined in `llm-wiki-common`
- Repositories: Spring Data JPA interfaces, named `{Entity}Repository`
- Composite keys: use `@IdClass` (e.g., `PageTagId` for `PageTag`)

## Anti-Patterns

- Don't add business logic to entities — use service layer
- Don't add Spring Data JDBC or MyBatis annotations — JPA only
- Don't reference adapter/web/domain from here — domain is a leaf in the dependency graph
- Don't use `Long` auto-increment IDs — UUID only
