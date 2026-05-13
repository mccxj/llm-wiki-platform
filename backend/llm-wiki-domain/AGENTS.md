# llm-wiki-domain — Domain Layer

**Module:** `com.llmwiki:llm-wiki-domain`
**Dependencies:** `llm-wiki-common`

## Overview

JPA entities + Spring Data repositories. Organized by domain concept.

## Package Structure

```
com.llmwiki.domain
├── sync/                      # Wiki source sync entities
│   ├── entity/                # WikiSource, SyncLog, RawDocument
│   └── repository/            # JPA repositories
├── processing/                # Pipeline processing
│   ├── entity/                # ProcessingLog, DeadLetterQueue
│   └── repository/            # JPA repositories
├── graph/                     # Knowledge graph
│   ├── entity/                # KgNode, KgEdge, KgVector
│   ├── repository/            # JPA repositories (KgNodeRepository, KgEdgeRepository, KgVectorRepository)
│   └── converter/             # Custom Hibernate UserType for VECTOR + deprecated FloatArrayToJsonConverter
├── page/                      # Generated pages
│   ├── entity/                # Page, PageLink, PageTag
│   └── repository/            # JPA repositories
├── approval/                  # Approval queue
│   ├── entity/                # ApprovalQueue, ApprovalAudit
│   └── repository/            # JPA repositories
├── example/                   # Entity examples
│   ├── entity/                # EntityExample
│   └── repository/            # JPA repositories
├── config/                    # System configuration
│   ├── entity/                # SystemConfig
│   └── repository/            # JPA repositories
├── audit/                     # Audit logging
│   ├── entity/                # AuditLog
│   └── repository/            # JPA repositories
└── maintenance/               # Maintenance entities
    ├── entity/                # MaintenanceReportLog, MaintenanceReport, DuplicateGroup, ConsistencyReport
    └── repository/            # JPA repositories
```

## VECTOR Column Convention

MariaDB 11.8+ native VECTOR columns require a **Hibernate UserType** because the JDBC driver must use `setObject(float[])`, not `setString()`.

### Correct approach (ALWAYS use this):

```java
import org.hibernate.annotations.Type;

@Type(MariaDBVectorType.class)
@Column(nullable = false, columnDefinition = "VECTOR(1536)")
private float[] vector;
```

- `MariaDBVectorType` (in `graph/converter/`) implements `UserType<float[]>` — writes via `PreparedStatement.setObject(index, float[])`
- The `columnDefinition` is required for Flyway schema validation (`ddl-auto: validate`)

### Incorrect approach (DO NOT use):

```java
// BROKEN at runtime — AttributeConverter uses setString(), not setObject()
@Convert(converter = FloatArrayToJsonConverter.class)
private float[] vector;
```

The old `FloatArrayToJsonConverter` is deprecated and no longer wired into any entity.

### Native SQL for vector search:

```sql
-- Similarity search
SELECT v.node_id, VEC_DISTANCE(v.vector, VEC_FromText(:queryVector)) AS distance
FROM kg_vectors v ORDER BY distance ASC LIMIT :limit

-- Vector insert (when bypassing JPA)
INSERT INTO kg_vectors (node_id, vector, model) VALUES (?, VEC_FromText(?), ?)
```

## Repository Notes

- All repositories extend `JpaRepository<T, UUID>`
- **KgVectorRepository**: Exposes `findByNodeId()` and `findByNodeIdNot()` — vector similarity filtering is done at the service layer (to stay DB-agnostic for H2-based tests)
- Avoid native queries in repositories when possible — use `EntityManager` in service layer for DB-specific SQL
