# LLM Wiki 自动化平台 — 技术设计文档

> 版本：v1.0 | 日期：2026-05-01 | 基于 PRD v1.0

---

## 1. 技术选型总览

| 层次 | 技术 | 说明 |
|---|---|---|
| 后端框架 | Spring Boot 3.x (Java 17+) | 主流 Java 生态 |
| ORM | MyBatis-Plus | 灵活 SQL 控制，适合复杂查询 |
| 数据库 | MariaDB 11.8+ (原生 VECTOR) | 关系数据 + 向量检索 |
| 缓存 | Caffeine (本地) | 配置缓存、评分缓存 |
| 调度 | Spring Scheduler + db-scheduler | 定时同步任务 |
| 前端 | React 18 + Ant Design 5.x | PRD 确认 |
| 图谱可视化 | Cytoscape.js | 性能优于 D3 用于图谱 |
| 构建 | Maven (后端) + Vite (前端) | |
| 部署 | Docker Compose | app + db + frontend |

---

## 2. 项目骨架

### 2.1 后端模块结构

```
llm-wiki-platform/
├── llm-wiki-common/              # 公共模块
│   ├── src/main/java/com/llmwiki/common/
│   │   ├── model/                 # 通用模型
│   │   │   ├── dto/               # 通用 DTO
│   │   │   ├── enums/             # 枚举
│   │   │   └── vo/                # 视图对象
│   │   ├── exception/             # 异常定义
│   │   ├── util/                  # 工具类
│   │   └── constants/             # 常量
│   └── pom.xml
│
├── llm-wiki-adapter/             # 外部依赖适配器模块
│   ├── src/main/java/com/llmwiki/adapter/
│   │   ├── ai/                    # AI API 适配器
│   │   │   ├── AiApiClient.java           # 抽象接口
│   │   │   ├── OpenAiCompatibleClient.java # OpenAI兼容实现
│   │   │   └── dto/               # AI请求/响应DTO
│   │   ├── embedding/             # Embedding 适配器
│   │   │   ├── EmbeddingClient.java       # 抽象接口
│   │   │   └── OpenAiEmbeddingClient.java # OpenAI Embedding实现
│   │   └── wiki/                  # Wiki 数据源适配器
│   │       ├── WikiSourceAdapter.java     # 抽象接口
│   │       ├── WikiSourceAdapterFactory.java # 工厂
│   │       └── dto/               # 原始文档DTO
│   └── pom.xml
│
├── llm-wiki-domain/              # 领域模块
│   ├── src/main/java/com/llmwiki/domain/
│   │   ├── page/                  # 页面领域
│   │   │   ├── entity/            # Page, PageTag, PageLink 实体
│   │   │   ├── repository/        # Repository 接口
│   │   │   └── service/           # PageDomainService
│   │   ├── graph/                 # 知识图谱领域
│   │   │   ├── entity/            # KgNode, KgEdge, KgVector 实体
│   │   │   ├── repository/
│   │   │   └── service/
│   │   ├── approval/              # 审批领域
│   │   │   ├── entity/            # ApprovalQueue 实体
│   │   │   ├── repository/
│   │   │   └── service/
│   │   ├── sync/                  # 同步领域
│   │   │   ├── entity/            # RawDocument, WikiSource, SyncLog 实体
│   │   │   ├── repository/
│   │   │   └── service/
│   │   └── processing/            # 处理流水线领域
│   │       ├── entity/            # ProcessingLog 实体
│   │       ├── repository/
│   │       └── service/
│   └── pom.xml
│
├── llm-wiki-service/             # 应用服务模块
│   ├── src/main/java/com/llmwiki/service/
│   │   ├── sync/                  # 同步服务
│   │   │   └── SyncService.java
│   │   ├── pipeline/              # 处理流水线编排
│   │   │   ├── DocumentPipeline.java      # 流水线编排器
│   │   │   ├── PipelineStep.java          # 步骤抽象
│   │   │   ├── steps/             # 各步骤实现
│   │   │   │   ├── TextCleanStep.java
│   │   │   │   ├── ScoringStep.java
│   │   │   │   ├── EntityExtractionStep.java
│   │   │   │   ├── ConceptExtractionStep.java
│   │   │   │   ├── GraphMatchingStep.java
│   │   │   │   ├── PageGenerationStep.java
│   │   │   │   ├── CrossLinkStep.java
│   │   │   │   └── ConsistencyCheckStep.java
│   │   ├── scoring/               # 评分服务
│   │   │   └── ScoringService.java
│   │   ├── search/                # 搜索服务
│   │   │   └── SearchService.java
│   │   ├── qa/                    # 问答服务
│   │   │   └── QaService.java
│   │   ├── graph/                 # 图谱服务
│   │   │   └── GraphService.java
│   │   ├── approval/              # 审批服务
│   │   │   └── ApprovalService.java
│   │   └── maintenance/           # 维护服务
│   │       └── MaintenanceService.java
│   └── pom.xml
│
├── llm-wiki-web/                 # Web 接口模块
│   ├── src/main/java/com/llmwiki/web/
│   │   ├── controller/            # REST 控制器
│   │   │   ├── SyncController.java
│   │   │   ├── ApprovalController.java
│   │   │   ├── SearchController.java
│   │   │   ├── QaController.java
│   │   │   ├── GraphController.java
│   │   │   ├── PageController.java
│   │   │   └── AdminController.java
│   │   ├── config/                # 配置类
│   │   │   ├── SecurityConfig.java
│   │   │   ├── SchedulerConfig.java
│   │   │   └── WebMvcConfig.java
│   │   ├── security/              # JWT 认证
│   │   └── LlmWikiApplication.java # 启动类
│   └── pom.xml
│
└── pom.xml                        # 父 POM
```

### 2.2 前端项目结构

```
frontend/
├── src/
│   ├── api/                       # API 调用层
│   │   ├── sync.ts
│   │   ├── approval.ts
│   │   ├── search.ts
│   │   ├── qa.ts
│   │   ├── graph.ts
│   │   ├── page.ts
│   │   └── admin.ts
│   ├── components/                # 通用组件
│   │   ├── Layout/                # 布局组件
│   │   ├── DiffViewer/            # Diff 对比组件
│   │   └── SearchHighlight/       # 搜索高亮组件
│   ├── pages/                     # 页面
│   │   ├── Search/                # 搜索页
│   │   ├── Qa/                    # 问答页
│   │   ├── Graph/                 # 图谱可视化页
│   │   ├── Approval/              # 审批页
│   │   ├── PageDetail/            # 页面详情
│   │   └── Admin/                 # 管理后台
│   │       ├── Config.tsx
│   │       ├── Sync.tsx
│   │       └── Maintenance.tsx
│   ├── hooks/                     # 自定义 Hooks
│   ├── store/                     # 状态管理 (Zustand)
│   ├── types/                     # TypeScript 类型定义
│   └── utils/                     # 工具函数
├── package.json
└── vite.config.ts
```

---

## 3. 数据库设计

### 3.1 建表脚本

```sql
-- MariaDB 11.8+ 原生 VECTOR 类型无需扩展

-- ========== 系统配置 ==========
CREATE TABLE system_config (
    key         VARCHAR(100) PRIMARY KEY,
    value       TEXT NOT NULL,
    description VARCHAR(500),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

-- ========== 数据源配置 ==========
CREATE TABLE wiki_sources (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    adapter_class   VARCHAR(255) NOT NULL,
    config          JSONB NOT NULL DEFAULT '{}',
    sync_cron       VARCHAR(100) DEFAULT '0 */6 * * *',
    last_sync_at    TIMESTAMPTZ,
    enabled         BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ========== 原始文档 ==========
CREATE TABLE raw_documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id       VARCHAR(255) NOT NULL,
    source_name     VARCHAR(255),
    title           VARCHAR(500),
    content         TEXT,
    content_hash    VARCHAR(64) NOT NULL,
    source_url      TEXT,
    ingested_at     TIMESTAMPTZ DEFAULT NOW(),
    last_checked_at TIMESTAMPTZ,
    UNIQUE(source_id, source_name)
);
CREATE INDEX idx_raw_hash ON raw_documents(content_hash);

-- ========== 页面 ==========
CREATE TABLE pages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug            VARCHAR(255) UNIQUE NOT NULL,
    title           VARCHAR(500) NOT NULL,
    content         TEXT,
    page_type       VARCHAR(20) NOT NULL, -- ENTITY, CONCEPT, COMPARISON, QUERY, RAW_SOURCE
    status          VARCHAR(20) DEFAULT 'PENDING_APPROVAL', -- PENDING_APPROVED, APPROVED, REJECTED, ARCHIVED
    confidence      VARCHAR(10), -- HIGH, MEDIUM, LOW
    contested       BOOLEAN DEFAULT FALSE,
    ai_score        DECIMAL(3,1),
    ai_score_detail JSONB,
    source_id       VARCHAR(255),
    approved_by     UUID,
    approved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_pages_type ON pages(page_type);
CREATE INDEX idx_pages_status ON pages(status);
CREATE INDEX idx_pages_score ON pages(ai_score);

-- ========== 页面标签 ==========
CREATE TABLE page_tags (
    page_id     UUID NOT NULL REFERENCES pages(id) ON DELETE CASCADE,
    tag         VARCHAR(100) NOT NULL,
    PRIMARY KEY (page_id, tag)
);
CREATE INDEX idx_tags_tag ON page_tags(tag);

-- ========== 页面交叉链接 ==========
CREATE TABLE page_links (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_page_id  UUID NOT NULL REFERENCES pages(id) ON DELETE CASCADE,
    target_page_id  UUID NOT NULL REFERENCES pages(id) ON DELETE CASCADE,
    link_type       VARCHAR(20) DEFAULT 'RELATED_TO',
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(source_page_id, target_page_id, link_type)
);
CREATE INDEX idx_links_source ON page_links(source_page_id);
CREATE INDEX idx_links_target ON page_links(target_page_id);

-- ========== 页面来源 ==========
CREATE TABLE page_sources (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    page_id         UUID NOT NULL REFERENCES pages(id) ON DELETE CASCADE,
    raw_document_id UUID NOT NULL REFERENCES raw_documents(id),
    source_excerpt  TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ========== 知识图谱节点 ==========
CREATE TABLE kg_nodes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(500) NOT NULL,
    node_type       VARCHAR(20) NOT NULL,
    description     TEXT,
    page_id         UUID REFERENCES pages(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_kg_nodes_type ON kg_nodes(node_type);
CREATE INDEX idx_kg_nodes_name ON kg_nodes(name);

-- ========== 知识图谱边 ==========
CREATE TABLE kg_edges (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_node_id  UUID NOT NULL REFERENCES kg_nodes(id) ON DELETE CASCADE,
    target_node_id  UUID NOT NULL REFERENCES kg_nodes(id) ON DELETE CASCADE,
    edge_type       VARCHAR(20) NOT NULL,
    weight          DECIMAL(3,2) DEFAULT 0.50,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(source_node_id, target_node_id, edge_type)
);
CREATE INDEX idx_kg_edges_source ON kg_edges(source_node_id);
CREATE INDEX idx_kg_edges_target ON kg_edges(target_node_id);

-- ========== 知识图谱向量 ==========
-- 向量维度根据 embedding 模型调整，这里以 1536 为例
CREATE TABLE kg_vectors (
    node_id     UUID PRIMARY KEY REFERENCES kg_nodes(id) ON DELETE CASCADE,
    vector      vector(1536) NOT NULL,
    model       VARCHAR(100) DEFAULT 'text-embedding-ada-002',
    created_at  TIMESTAMPTZ DEFAULT NOW()
);
-- IVFFlat 索引，lists 参数根据数据量调整
CREATE INDEX idx_kg_vectors_ann ON kg_vectors USING ivfflat (vector vector_cosine_ops) WITH (lists = 100);

-- ========== 审批队列 ==========
CREATE TABLE approval_queue (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       UUID NOT NULL,
    action          VARCHAR(20) NOT NULL, -- CREATE, UPDATE, DELETE
    before_value    JSONB,
    after_value     JSONB,
    summary         TEXT,
    status          VARCHAR(20) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    reviewer_id     UUID,
    reviewed_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_approval_status ON approval_queue(status);

-- ========== 同步日志 ==========
CREATE TABLE sync_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id       UUID REFERENCES wiki_sources(id),
    started_at      TIMESTAMPTZ NOT NULL,
    finished_at     TIMESTAMPTZ,
    fetched_count   INT DEFAULT 0,
    processed_count INT DEFAULT 0,
    skipped_count   INT DEFAULT 0,
    failed_count    INT DEFAULT 0,
    status          VARCHAR(20) DEFAULT 'RUNNING',
    error_message   TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ========== 处理流水线日志 ==========
CREATE TABLE processing_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    raw_document_id UUID REFERENCES raw_documents(id),
    step            VARCHAR(50) NOT NULL,
    status          VARCHAR(20) NOT NULL, -- SUCCESS, FAILED, SKIPPED
    detail          JSONB,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_proc_log_doc ON processing_log(raw_document_id);

-- ========== 默认配置 ==========
INSERT INTO system_config(key, value, description) VALUES
('ai_score_threshold', '5.0', 'AI综合评分阈值，低于此值的文档跳过处理'),
('ai_model', 'gpt-4o-mini', '使用的AI模型'),
('embedding_model', 'text-embedding-ada-002', '使用的Embedding模型'),
('sync_batch_size', '20', '每批同步处理的文档数'),
('scoring_weights', '{"information_density":0.30,"entity_richness":0.25,"knowledge_independence":0.20,"structure_integrity":0.15,"timeliness":0.10}', '评分维度权重');
```

### 3.2 向量检索查询示例

```sql
-- 向量相似度搜索（余弦距离）
SELECT n.id, n.name, n.node_type, n.description,
       1 - (v.vector <=> :query_vector) AS similarity
FROM kg_vectors v
JOIN kg_nodes n ON n.id = v.node_id
WHERE n.node_type = ANY(:types)
ORDER BY v.vector <=> :query_vector
LIMIT :limit;
```

---

## 4. 数据源适配器设计

### 4.1 抽象接口

```java
// adapter/wiki/WikiSourceAdapter.java
public interface WikiSourceAdapter {
    /** 测试连接 */
    boolean testConnection();
    /** 获取增量变更 */
    List<RawDocumentDTO> fetchChanges(Instant since);
    /** 获取单个页面 */
    RawDocumentDTO fetchPage(String pageId);
}

// adapter/wiki/dto/RawDocumentDTO.java
public class RawDocumentDTO {
    private String sourceId;
    private String title;
    private String content;       // 统一为 Markdown
    private String sourceUrl;
    private Instant lastModified;
    private String format;        // MARKDOWN, HTML, RICH_TEXT
}
```

### 4.2 工厂模式

```java
// adapter/wiki/WikiSourceAdapterFactory.java
@Component
public class WikiSourceAdapterFactory {
    private final Map<String, WikiSourceAdapter> adapters;

    public WikiSourceAdapterFactory(List<WikiSourceAdapter> adapterList) {
        this.adapters = adapterList.stream()
            .collect(Collectors.toMap(
                a -> a.getClass().getAnnotation(AdapterType.class).value(),
                Function.identity()
            ));
    }

    public WikiSourceAdapter getAdapter(String adapterClass) {
        return adapters.values().stream()
            .filter(a -> a.getClass().getName().equals(adapterClass))
            .findFirst()
            .orElseThrow(() -> new AdapterNotFoundException(adapterClass));
    }
}
```

### 4.3 接入方实现示例

接入方只需实现 `WikiSourceAdapter` 接口并标注 `@AdapterType`，Spring 自动注入工厂。

---

## 5. 同步服务设计

### 5.1 调度配置

```java
// web/config/SchedulerConfig.java
@Configuration
@EnableScheduling
public class SchedulerConfig {
    // 动态注册同步任务，从 wiki_sources 表读取 cron 配置
}

// 使用 db-scheduler 实现持久化调度（防止重启丢失）
// 或者使用 Spring Scheduler 动态注册
```

### 5.2 同步流程

```java
// service/sync/SyncService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class SyncService {
    private final WikiSourceRepository sourceRepo;
    private final RawDocumentRepository rawDocRepo;
    private final SyncLogRepository syncLogRepo;
    private final WikiSourceAdapterFactory adapterFactory;
    private final DocumentPipeline pipeline;

    @Transactional
    public SyncLog syncSource(UUID sourceId) {
        WikiSource source = sourceRepo.findById(sourceId).orElseThrow();
        WikiSourceAdapter adapter = adapterFactory.getAdapter(source.getAdapterClass());

        SyncLog syncLog = SyncLog.start(sourceId);
        try {
            // 1. 增量拉取
            List<RawDocumentDTO> changes = adapter.fetchChanges(source.getLastSyncAt());

            // 2. 去重 + 漂移检测
            int processed = 0, skipped = 0, failed = 0;
            for (RawDocumentDTO doc : changes) {
                String hash = DigestUtils.sha256Hex(doc.getContent());
                Optional<RawDocument> existing = rawDocRepo.findBySourceId(doc.getSourceId());

                if (existing.isPresent() && existing.get().getContentHash().equals(hash)) {
                    skipped++; // 内容未变化
                    continue;
                }

                // 3. 保存原始文档
                RawDocument rawDoc = saveRawDocument(doc, hash, existing.orElse(null));

                // 4. 提交到处理流水线（异步）
                pipeline.submit(rawDoc);
                processed++;
            }

            syncLog.complete(processed, skipped, failed);
            source.setLastSyncAt(Instant.now());
            sourceRepo.save(source);

        } catch (Exception e) {
            syncLog.fail(e.getMessage());
        }
        return syncLogRepo.save(syncLog);
    }
}
```

### 5.3 手动触发

```java
// web/controller/SyncController.java
@RestController
@RequestMapping("/api/sync")
public class SyncController {
    @PostMapping("/trigger")
    public SyncLog triggerSync(@RequestParam UUID sourceId) {
        return syncService.syncSource(sourceId);
    }
}
```

---

## 6. AI 评分模块设计

### 6.1 抽象接口

```java
// adapter/ai/AiApiClient.java
public interface AiApiClient {
    /** 评分 */
    ScoreResult score(String content);
    /** 实体提取 */
    ExtractionResult extractEntities(String content);
    /** 概念提取 */
    ExtractionResult extractConcepts(String content);
    /** 通用对话 */
    String chat(String systemPrompt, String userMessage);
}

// adapter/ai/dto/ScoreResult.java
public class ScoreResult {
    private Map<String, Integer> scores;  // 各维度分
    private BigDecimal overallScore;       // 综合分
    private String reason;
    private List<String> keyEntities;
    private List<String> suggestedTags;
}
```

### 6.2 OpenAI 兼容实现

```java
// adapter/ai/OpenAiCompatibleClient.java
@Component
@AdapterType("openai-compatible")
public class OpenAiCompatibleClient implements AiApiClient {
    private final WebClient webClient;

    public OpenAiCompatibleClient(@Value("${ai.api.base-url}") String baseUrl,
                                   @Value("${ai.api.key}") String apiKey,
                                   WebClient.Builder builder) {
        this.webClient = builder
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .build();
    }

    @Override
    public ScoreResult score(String content) {
        String systemPrompt = """
            你是一个文档质量评估专家。请从5个维度评分（每项1-10分），加权计算综合分。
            维度权重：信息密度30%、实体丰富度25%、知识独立性20%、结构完整性15%、时效相关性10%。
            返回JSON：{scores:{...},overall_score:number,reason:string,key_entities:[],suggested_tags:[]}
            """;

        ChatResponse response = callChat(systemPrompt, "文档内容：\n---\n" + content);
        return parseScoreResult(response.getContent());
    }

    private ChatResponse callChat(String system, String user) {
        return webClient.post()
            .uri("/chat/completions")
            .bodyValue(Map.of(
                "model", modelName,
                "messages", List.of(
                    Map.of("role", "system", "content", system),
                    Map.of("role", "user", "content", user)
                ),
                "response_format", Map.of("type", "json_object"),
                "temperature", 0.1
            ))
            .retrieve()
            .bodyToMono(ChatResponse.class)
            .timeout(Duration.ofSeconds(30))
            .block();
    }
}
```

### 6.3 评分服务

```java
// service/scoring/ScoringService.java
@Service
@RequiredArgsConstructor
public class ScoringService {
    private final AiApiClient aiClient;
    private final SystemConfigRepository configRepo;

    public ScoreResult scoreDocument(String content) {
        return aiClient.score(content);
    }

    public boolean passesThreshold(ScoreResult result) {
        BigDecimal threshold = new BigDecimal(
            configRepo.findValueByKey("ai_score_threshold").orElse("5.0")
        );
        return result.getOverallScore().compareTo(threshold) >= 0;
    }
}
```

---

## 7. 处理流水线设计

### 7.1 流水线编排器

```java
// service/pipeline/DocumentPipeline.java
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentPipeline {
    private final TextCleanStep textCleanStep;
    private final ScoringStep scoringStep;
    private final EntityExtractionStep entityExtractionStep;
    private final ConceptExtractionStep conceptExtractionStep;
    private final GraphMatchingStep graphMatchingStep;
    private final PageGenerationStep pageGenerationStep;
    private final CrossLinkStep crossLinkStep;
    private final ConsistencyCheckStep consistencyCheckStep;
    private final ApprovalService approvalService;

    private final List<PipelineStep> steps;

    @Async("pipelineExecutor")
    public CompletableFuture<PipelineResult> submit(RawDocument rawDoc) {
        PipelineContext context = new PipelineContext(rawDoc);
        try {
            for (PipelineStep step : steps) {
                StepResult result = step.execute(context);
                logStep(context, step.getName(), result);
                if (result.getStatus() == StepStatus.SKIPPED) {
                    return CompletableFuture.completedFuture(
                        PipelineResult.skipped(step.getName(), result.getReason()));
                }
                if (result.getStatus() == StepStatus.FAILED) {
                    return CompletableFuture.completedFuture(
                        PipelineResult.failed(step.getName(), result.getError()));
                }
            }
            // 提交审批
            approvalService.submitForApproval(context);
            return CompletableFuture.completedFuture(PipelineResult.success(context));
        } catch (Exception e) {
            log.error("Pipeline failed for doc {}", rawDoc.getId(), e);
            return CompletableFuture.completedFuture(PipelineResult.failed("unknown", e.getMessage()));
        }
    }
}
```

### 7.2 步骤抽象

```java
// service/pipeline/PipelineStep.java
public interface PipelineStep {
    String getName();
    StepResult execute(PipelineContext context);
    default int getOrder() { return 0; }
}

// service/pipeline/PipelineContext.java
public class PipelineContext {
    private final RawDocument rawDocument;
    private String cleanedContent;
    private ScoreResult scoreResult;
    private List<EntityInfo> entities;
    private List<ConceptInfo> concepts;
    private List<KgNode> matchedNodes;
    private Page generatedPage;
    private List<PageLink> generatedLinks;
    private List<ConsistencyIssue> consistencyIssues;
    // ... getters & setters
}
```

### 7.3 评分步骤

```java
// service/pipeline/steps/ScoringStep.java
@Component
@Order(2)
public class ScoringStep implements PipelineStep {
    private final ScoringService scoringService;

    @Override
    public StepResult execute(PipelineContext context) {
        ScoreResult result = scoringService.scoreDocument(context.getCleanedContent());
        context.setScoreResult(result);

        if (!scoringService.passesThreshold(result)) {
            return StepResult.skipped("AI score " + result.getOverallScore()
                + " below threshold");
        }
        return StepResult.success();
    }
}
```

### 7.4 实体提取步骤

```java
// service/pipeline/steps/EntityExtractionStep.java
@Component
@Order(3)
public class EntityExtractionStep implements PipelineStep {
    private final AiApiClient aiClient;

    @Override
    public StepResult execute(PipelineContext context) {
        ExtractionResult result = aiClient.extractEntities(context.getCleanedContent());
        context.setEntities(result.getEntities());
        return StepResult.success();
    }
}
```

---

## 8. 知识图谱模块设计

### 8.1 节点匹配逻辑

```java
// service/pipeline/steps/GraphMatchingStep.java
@Component
public class GraphMatchingStep implements PipelineStep {
    private final KgNodeRepository nodeRepo;
    private final KgVectorRepository vectorRepo;
    private final EmbeddingClient embeddingClient;

    @Override
    public StepResult execute(PipelineContext context) {
        List<KgNode> matchedNodes = new ArrayList<>();

        for (EntityInfo entity : context.getEntities()) {
            // 1. 精确匹配
            Optional<KgNode> exact = nodeRepo.findByNameIgnoreCase(entity.getName());
            if (exact.isPresent()) {
                matchedNodes.add(exact.get());
                continue;
            }

            // 2. 向量相似度匹配
            float[] embedding = embeddingClient.embed(entity.getName());
            List<KgNode> similar = vectorRepo.findSimilar(embedding, 0.85f, 3);
            if (!similar.isEmpty()) {
                matchedNodes.add(similar.get(0)); // 取最相似的
            } else {
                // 3. 创建新节点
                KgNode newNode = createNode(entity);
                matchedNodes.add(newNode);
            }
        }

        context.setMatchedNodes(matchedNodes);
        return StepResult.success();
    }
}
```

### 8.2 向量存储 Repository

```java
// domain/graph/repository/KgVectorRepository.java
@Repository
public interface KgVectorRepository extends JpaRepository<KgVector, UUID> {

    @Query(value = """
        SELECT n.id, n.name, n.node_type, n.description,
               1 - (v.vector <=> CAST(:queryVector AS vector)) AS similarity
        FROM kg_vectors v
        JOIN kg_nodes n ON n.id = v.node_id
        WHERE 1 - (v.vector <=> CAST(:queryVector AS vector)) >= :threshold
        ORDER BY v.vector <=> CAST(:queryVector AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<KgNodeSimilarity> findSimilar(String queryVector, float threshold, int limit);
}
```

---

## 9. 搜索与问答模块设计

### 9.1 语义搜索

```java
// service/search/SearchService.java
@Service
@RequiredArgsConstructor
public class SearchService {
    private final EmbeddingClient embeddingClient;
    private final KgVectorRepository vectorRepo;
    private final PageRepository pageRepo;

    public SearchResult search(SearchRequest request) {
        // 1. 生成查询向量
        float[] queryVector = embeddingClient.embed(request.getQuery());

        // 2. 向量检索
        List<KgNodeSimilarity> nodes = vectorRepo.findSimilar(
            toPgVector(queryVector), 0.7f, request.getLimit());

        // 3. 组装结果
        return SearchResult.builder()
            .items(nodes.stream().map(this::toSearchItem).toList())
            .total(nodes.size())
            .build();
    }
}
```

### 9.2 问答服务

```java
// service/qa/QaService.java
@Service
@RequiredArgsConstructor
public class QaService {
    private final SearchService searchService;
    private final AiApiClient aiClient;
    private final PageRepository pageRepo;

    public QaResult ask(String question) {
        // 1. 向量检索相关内容
        SearchResult searchResult = searchService.search(
            SearchRequest.builder().query(question).limit(5).build());

        // 2. 判断知识库是否有足够内容
        boolean hasRelevantContent = searchResult.getItems().stream()
            .anyMatch(item -> item.getSimilarity() > 0.75);

        // 3. 构建上下文
        String context = searchResult.getItems().stream()
            .filter(item -> item.getPageContent() != null)
            .map(item => item.getPageTitle() + ":\n" + item.getPageContent())
            .collect(Collectors.joining("\n---\n"));

        // 4. 生成回答
        boolean aiFallenBack = !hasRelevantContent;
        String answer = generateAnswer(question, context, aiFallenBack);

        return QaResult.builder()
            .answer(answer)
            .sources(toSources(searchResult))
            .aiFallenBack(aiFallenBack)
            .build();
    }

    private String generateAnswer(String question, String context, boolean fallback) {
        String systemPrompt = fallback
            ? "你是一个知识助手。知识库中没有找到确切依据，请基于你的知识回答，并明确标注。"
            : "你是一个知识助手。请严格基于以下知识库内容回答问题，不要编造。引用来源。";

        String userMsg = "知识库内容：\n" + context + "\n\n问题：" + question;
        return aiClient.chat(systemPrompt, userMsg);
    }
}
```

---

## 10. 审批模块设计

### 10.1 审批服务

```java
// service/approval/ApprovalService.java
@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalService {
    private final ApprovalQueueRepository approvalRepo;
    private final PageRepository pageRepo;

    /** 提交变更到审批队列 */
    public ApprovalQueue submitForApproval(PipelineContext context) {
        ApprovalQueue item = ApprovalQueue.builder()
            .entityType("PAGE")
            .entityId(context.getGeneratedPage().getId())
            .action(context.isUpdate() ? "UPDATE" : "CREATE")
            .beforeValue(context.getBeforeValue())
            .afterValue(context.getAfterValue())
            .summary(generateSummary(context))
            .status("PENDING")
            .build();
        return approvalRepo.save(item);
    }

    /** 审批通过 */
    public void approve(UUID approvalId, UUID reviewerId) {
        ApprovalQueue item = approvalRepo.findById(approvalId).orElseThrow();
        item.setStatus("APPROVED");
        item.setReviewerId(reviewerId);
        item.setReviewedAt(Instant.now());
        approvalRepo.save(item);

        // 生效变更
        Page page = pageRepo.findById(item.getEntityId()).orElseThrow();
        page.setStatus("APPROVED");
        page.setApprovedBy(reviewerId);
        page.setApprovedAt(Instant.now());
        pageRepo.save(page);
    }
}
```

### 10.2 Diff 对比

```java
// util/DiffUtil.java
public class DiffUtil {
    public static String generateDiff(String before, String after) {
        // 使用 google-diff-match-patch 或 java-diff-utils
        Patch<String> patch = DiffUtils.diff(
            Arrays.asList(before.split("\n")),
            Arrays.asList(after.split("\n"))
        );
        return patch.getDeltas().stream()
            .map(Delta::toString)
            .collect(Collectors.joining("\n"));
    }
}
```

---

## 11. 前端设计

### 11.1 技术栈

| 技术 | 说明 |
|---|---|
| React 18 | 核心框架 |
| Ant Design 5.x | UI 组件库 |
| Zustand | 轻量状态管理 |
| React Router v6 | 路由 |
| Axios | HTTP 请求 |
| Cytoscape.js | 知识图谱可视化 (通过 react-cytoscapejs) |
| React Markdown | Markdown 渲染 |
| Vite | 构建工具 |
| TypeScript | 类型安全 |

### 11.2 页面路由

```typescript
// 路由配置
const routes = [
  { path: '/', element: <SearchPage /> },
  { path: '/qa', element: <QaPage /> },
  { path: '/graph', element: <GraphPage /> },
  { path: '/pages/:id', element: <PageDetail /> },
  { path: '/approval', element: <ApprovalPage />, admin: true },
  { path: '/admin', element: <AdminLayout />, admin: true,
    children: [
      { path: 'config', element: <ConfigPage /> },
      { path: 'sync', element: <SyncPage /> },
      { path: 'maintenance', element: <MaintenancePage /> },
    ]
  },
];
```

### 11.3 核心页面组件

#### 搜索页

```tsx
// pages/Search/index.tsx
const SearchPage: React.FC = () => {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<SearchItem[]>([]);
  const [filters, setFilters] = useState({ types: [], tags: [] });

  const handleSearch = async () => {
    const res = await searchApi.search({ query, ...filters });
    setResults(res.items);
  };

  return (
    <Layout>
      <SearchBar value={query} onChange={setQuery} onSearch={handleSearch} />
      <FilterPanel filters={filters} onChange={setFilters} />
      <ResultList items={results} />
    </Layout>
  );
};
```

#### 知识图谱页

```tsx
// pages/Graph/index.tsx
import CytoscapeComponent from 'react-cytoscapejs';

const GraphPage: React.FC = () => {
  const [elements, setElements] = useState([]);
  const [selectedNode, setSelectedNode] = useState(null);

  useEffect(() => {
    graphApi.getGraph().then(data => {
      setElements([...data.nodes.map(n => ({ data: n })), ...data.edges.map(e => ({ data: e }))]);
    });
  }, []);

  const layout = { name: 'cose', padding: 10 };

  return (
    <Layout>
      <div style={{ display: 'flex', height: '100%' }}>
        <div style={{ flex: 1 }}>
          <CytoscapeComponent
            elements={elements}
            layout={layout}
            stylesheet={graphStyle}
            cy={(cy) => cy.on('tap', 'node', evt => setSelectedNode(evt.target.data()))}
          />
        </div>
        {selectedNode && <NodeDetailPanel node={selectedNode} />}
      </div>
    </Layout>
  );
};
```

#### 审批页

```tsx
// pages/Approval/index.tsx
const ApprovalPage: React.FC = () => {
  const [pendingList, setPendingList] = useState<ApprovalItem[]>([]);
  const [selectedItem, setSelectedItem] = useState<ApprovalItem>(null);

  return (
    <Layout>
      <Row gutter={16}>
        <Col span={8}>
          <List
            dataSource={pendingList}
            renderItem={item => (
              <List.Item onClick={() => setSelectedItem(item)}>
                <Tag>{item.action}</Tag> {item.summary}
              </List.Item>
            )}
          />
        </Col>
        <Col span={16}>
          {selectedItem && (
            <>
              <DiffViewer
                before={selectedItem.beforeValue}
                after={selectedItem.afterValue}
              />
              <Space>
                <Button type="primary" onClick={() => handleApprove(selectedItem.id)}>通过</Button>
                <Button danger onClick={() => handleReject(selectedItem.id)}>拒绝</Button>
              </Space>
            </>
          )}
        </Col>
      </Row>
    </Layout>
  );
};
```

#### 问答页

```tsx
// pages/Qa/index.tsx
const QaPage: React.FC = () => {
  const [messages, setMessages] = useState<QaMessage[]>([]);
  const [input, setInput] = useState('');

  const handleAsk = async () => {
    const question = input.trim();
    setMessages(prev => [...prev, { role: 'user', content: question }]);
    setInput('');

    const res = await qaApi.ask(question);
    setMessages(prev => [...prev, {
      role: 'assistant',
      content: res.answer,
      sources: res.sources,
      aiFallenBack: res.aiFallenBack,
    }]);
  };

  return (
    <Layout>
      <Chat messages={messages} />
      <Input.Search
        value={input}
        onChange={e => setInput(e.target.value)}
        onSearch={handleAsk}
        placeholder="输入你的问题..."
        enterButton="提问"
      />
    </Layout>
  );
};
```

### 11.4 状态管理

```typescript
// store/authStore.ts
import { create } from 'zustand';

interface AuthState {
  token: string | null;
  isAdmin: boolean;
  login: (token: string, isAdmin: boolean) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: localStorage.getItem('token'),
  isAdmin: localStorage.getItem('isAdmin') === 'true',
  login: (token, isAdmin) => {
    localStorage.setItem('token', token);
    localStorage.setItem('isAdmin', String(isAdmin));
    set({ token, isAdmin });
  },
  logout: () => {
    localStorage.clear();
    set({ token: null, isAdmin: false });
  },
}));
```

---

## 12. 配置管理

### 12.1 application.yml

```yaml
spring:
  datasource:
     url: jdbc:mariadb://db:3306/llmwiki
    username: llmwiki
    password: ${DB_PASSWORD}
  jackson:
    serialization:
      write-dates-as-timestamps: false

ai:
  api:
    base-url: ${AI_API_BASE_URL}
    key: ${AI_API_KEY}
  model: ${AI_MODEL:gpt-4o-mini}

embedding:
  model: ${EMBEDDING_MODEL:text-embedding-ada-002}

pipeline:
  thread-pool-size: 4
  retry-max-attempts: 3
  retry-delay-ms: 1000

sync:
  enabled: true
  default-cron: "0 */6 * * *"

# MyBatis-Plus 配置
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
```

### 12.2 Docker Compose

```yaml
version: '3.8'

services:
   db:
     image: mariadb:11.8
     environment:
       MARIADB_DATABASE: llmwiki
       MARIADB_USER: llmwiki
       MARIADB_PASSWORD: ${DB_PASSWORD}
       MARIADB_ROOT_PASSWORD: ${DB_PASSWORD}
     volumes:
       - mariadb_data:/var/lib/mysql
       - ./init.sql:/docker-entrypoint-initdb.d/init.sql
     ports:
       - "3306:3306"
     healthcheck:
      test: ["CMD-SHELL", "pg_isready -U llmwiki"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    build:
      context: ./backend
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      DB_PASSWORD: ${DB_PASSWORD}
      AI_API_BASE_URL: ${AI_API_BASE_URL}
      AI_API_KEY: ${AI_API_KEY}
      AI_MODEL: ${AI_MODEL:-gpt-4o-mini}
    depends_on:
      db:
        condition: service_healthy

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    ports:
      - "3000:80"
    depends_on:
      - app

volumes:
   mariadb_data:
```

---

## 13. 关键实现注意事项

### 13.1 MariaDB VECTOR 使用要点

1. **向量维度**必须与 embedding 模型匹配（ada-002 = 1536）
2. **VECTOR 类型**：使用 `VECTOR(n)` 定义列，`VEC_FromText()` 插入，`VEC_DISTANCE()` 计算距离
3. **距离函数**：`VEC_DISTANCE(vec1, vec2)` 计算欧氏距离，结果越小越相似
4. **Spring Data JPA** 不支持 VECTOR 类型，需要用 `@Query(nativeQuery=true)` 或 EntityManager 原生查询

### 13.2 流水线可靠性

1. **幂等性**：每个步骤基于 `raw_document_id` 检查是否已处理，避免重复
2. **重试机制**：AI 调用失败自动重试 3 次，指数退避
3. **死信队列**：3 次重试后仍失败，记录到 `processing_log`，标记 `FAILED`
4. **异步执行**：流水线通过 `@Async` 异步执行，不阻塞同步线程

### 13.3 性能优化

1. **AI 调用批处理**：实体提取和概念提取合并为一次 API 调用
2. **配置缓存**：`system_config` 使用 Caffeine 本地缓存，5 分钟过期
3. **向量检索**：MariaDB `VEC_DISTANCE()` 函数 + 原生 SQL 查询
4. **前端**：Ant Design 组件懒加载，Cytoscape 虚拟化渲染

---

## 14. 开发阶段建议

虽然 PRD 要求一步到位，但开发时可以按以下顺序逐步构建，每一步都可独立验证：

| 阶段 | 内容 | 可验证点 |
|---|---|---|
| **P0** | 项目骨架 + 数据库 + 数据源适配器 | 能跑起来，能连数据库 |
| **P1** | 同步服务 + 原始文档存储 | 能拉取文档并存入 |
| **P2** | AI 评分 + 流水线 | 能给文档打分，低分跳过 |
| **P3** | 实体提取 + 知识图谱 | 能提取实体，图谱有数据 |
| **P4** | 审批模块 | 能提交和审批变更 |
| **P5** | 搜索 + 问答 | 能搜索和问答 |
| **P6** | 前端全部 | 所有页面可操作 |
| **P7** | Docker 部署 | 一键启动完整系统 |
