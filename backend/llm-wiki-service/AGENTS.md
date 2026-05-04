# llm-wiki-service — Service Layer

**Module:** `com.llmwiki:llm-wiki-service`
**Depends on:** `llm-wiki-domain`, `llm-wiki-adapter`, `llm-wiki-common`

## Overview

Business logic layer. Orchestrates the AI processing pipeline, knowledge graph operations, search, sync, approval, and maintenance.

## Service Classes

| Service | Purpose | Key Methods |
|---------|---------|-------------|
| `PipelineService` | Document processing pipeline | `processDocument`, `retryDeadLetter` |
| `KnowledgeGraphService` | KG node/edge CRUD + BFS | `createNode`, `createEdge`, `deleteNode`, `findShortestPath`, `getGraphStats` |
| `SearchService` | Semantic search + NL QA | `search`, `searchByTag`, `searchByRelation`, `ask` |
| `SyncService` | Wiki source polling + raw doc ingestion | `syncSource`, `syncAll`, `computeContentHash` |
| `ApprovalService` | Approval workflow | `submitForApproval`, `approve`, `reject`, `listPending`, `listByStatus` |
| `MaintenanceService` | Stale page detection + consistency checks | `findStalePages`, `findContestedPages`, `checkIndexConsistency`, `generateReport` |
| `EntityExampleService` | Entity example CRUD | `findAll`, `findByType`, `createExample`, `updateExample`, `deleteExample`, `findById` |
| `ScoringService` | AI scoring + threshold mgmt | `scoreDocument`, `getDimensionThreshold`, `passesThreshold`, `passesDimensionThresholds`, `getScoreWeights` |

## Where to Look

| Task | Path |
|------|------|
| Service impl | `src/main/java/com/llmwiki/service/{domain}/` |
| Service tests | `src/test/java/com/llmwiki/service/{domain}/` |

## Conventions

- Services use `@RequiredArgsConstructor` for constructor injection (Lombok)
- Never call adapters directly — use interface types (`AiApiClient`, `EmbeddingClient`)
- Transaction boundaries: `@Transactional` on service methods that write
- `SystemConfigRepository` for runtime-configurable thresholds (scoring, staleness)

## Anti-Patterns

- Don't add HTTP/controller logic here — pure business logic only
- Don't catch and swallow exceptions — let them propagate to the controller
- Don't use `Thread.sleep` or blocking calls in reactive paths
