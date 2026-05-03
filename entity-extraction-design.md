# 实体提取模块 — 需求与设计文档

> 版本：v2.0 | 日期：2026-05-03 | 状态：已实现

---

## 1. 背景与目标

### 1.1 原始问题

初版实体提取实现存在以下缺陷（与 LangExtract 对比分析）：

| 问题 | 描述 |
|------|------|
| Zero-shot 提示词 | 无 few-shot examples，提取质量不稳定 |
| 无 Source Grounding | `EntityInfo` 缺少 `startOffset/endOffset`，无法追溯原文位置 |
| 分块策略粗糙 | 按 `\n\n` 分割，无重叠，边界实体被截断 |
| 去重过于简单 | 仅按 name 小写精确匹配，同名词不同义项被错误去重 |
| 实体/概念分离调用 | 两次独立 LLM 调用，丢失关联上下文，分类不一致 |
| 关系抽取过于简单 | `relatedEntities` 仅字符串列表，无关系类型、方向性、置信度 |
| 无多轮提取 | 单次 LLM 调用召回率受限 |

### 1.2 改进目标

引入 LangExtract 的核心设计理念，分阶段增强实体提取能力：

- **P0**：Source Grounding — 实体/概念增加原文位置信息
- **P0**：Few-shot 提示词 — 动态构建 examples 注入 prompt
- **P1**：改进分块策略 — 滑动窗口 + 句子边界感知 + 重叠区域
- **P1**：单次调用同时抽取实体+概念+关系
- **P2**：结构化关系类型 — `RelationInfo` 含类型、方向性、置信度
- **P2**：语义去重 — 用 embedding 相似度替代 name 精确匹配（待实现）

---

## 2. 架构设计

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                    Enhanced Entity Extraction                    │
│                                                                  │
│  RawDocument → scoreDocument()                                   │
│       │                                                          │
│       ▼                                                          │
│  extractEntities(content, examples)                              │
│       │                                                          │
│       ├─→ SlidingWindowChunker (8000 chars, 200 overlap)        │
│       │       │                                                  │
│       │       ▼                                                  │
│       │   Per-chunk LLM call (few-shot prompt)                   │
│       │       │                                                  │
│       │       ▼                                                  │
│       │   JSON Parse → EntityInfo (with startOffset/endOffset)   │
│       │                RelationInfo (source/target/type/conf)    │
│       │                                                          │
│       ├─→ Dedup by name (case-insensitive, keep first)           │
│       │                                                          │
│       ├─→ AlignmentResolver (3-tier fallback)                    │
│       │       Tier 1: Exact match (String.indexOf)               │
│       │       Tier 2: Fuzzy match (LCS similarity ≥ 0.75)        │
│       │       Tier 3: Token-level (density ≥ 1/3)                │
│       │                                                          │
│       ▼                                                          │
│  ExtractionResult { entities[], concepts[], relations[] }        │
│       │                                                          │
│       ▼                                                          │
│  matchKnowledgeGraph() → createEdgeIfNotExists()                 │
│       │                                                          │
│       ▼                                                          │
│  KgNode / KgEdge (with EdgeType + weight/confidence)             │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 模块依赖

```
llm-wiki-adapter (AiApiClient / OpenAiApiClient)
    ├── chunking/         — SlidingWindowChunker + TextChunk
    ├── resolver/         — AlignmentResolver (3-tier)
    ├── prompting/        — PromptTemplate (few-shot rendering)
    └── dto/              — ExtractionResult, RelationInfo, ExampleData

llm-wiki-service (PipelineService)
    └── example/          — EntityExampleService (CRUD for few-shot examples)

llm-wiki-common
    ├── enums/EdgeType    — Structured relation types
    ├── enums/AlignmentStatus — EXACT / FUZZY / GREATER / LESSER
    └── types/CharInterval — Offset range helper
```

---

## 3. 接口设计

### 3.1 AiApiClient 接口

```java
public interface AiApiClient {
    ScoreResult score(String content);

    // 实体提取（zero-shot）
    ExtractionResult extractEntities(String content);

    // 实体提取（few-shot，动态注入 examples）
    ExtractionResult extractEntities(String content, List<ExampleData> examples);

    // 概念提取（zero-shot）
    ExtractionResult extractConcepts(String content);

    // 概念提取（few-shot）
    ExtractionResult extractConcepts(String content, List<ExampleData> examples);

    // 统一提取：实体 + 概念 + 关系（单次 LLM 调用）
    UnifiedExtractionResult unifiedExtract(String content);

    String chat(String systemPrompt, String userMessage);
    boolean isAvailable();
}
```

### 3.2 核心 DTO

#### ExtractionResult

```
ExtractionResult
├── List<EntityInfo> entities
│   ├── String name
│   ├── String type            // PERSON | ORG | TECH | TOOL | OTHER
│   ├── String description
│   ├── List<String> relatedEntities
│   ├── Integer startOffset    // P0: source grounding — 原文起始字符位置
│   ├── Integer endOffset      // P0: source grounding — 原文结束字符位置
│   ├── AlignmentStatus alignmentStatus  // EXACT | FUZZY | GREATER | LESSER
│   └── Integer extractionIndex         // 第几个被提取的
│
├── List<ConceptInfo> concepts
│   ├── String name
│   ├── String description
│   ├── List<String> relatedEntities
│   ├── Integer startOffset
│   ├── Integer endOffset
│   ├── AlignmentStatus alignmentStatus
│   └── Integer extractionIndex
│
└── List<RelationInfo> relations      // P2: 结构化关系
    ├── String sourceEntity
    ├── String targetEntity
    ├── String relationType           // DEPENDS_ON | IS_A | PART_OF | ...
    └── Double confidence             // 0.0 ~ 1.0
```

#### UnifiedExtractionResult（单次调用合并抽取）

```
UnifiedExtractionResult
├── List<EntityInfo> entities         // name, type, description, relatedEntities
├── List<ConceptInfo> concepts        // name, description, relatedEntities
└── List<RelationInfo> relations      // sourceEntity, targetEntity, relationType, confidence
```

#### ExampleData（Few-shot 示例）

```
ExampleData
├── String text                       // 示例文本
└── List<LabeledExtraction> extractions
    ├── String extractionClass        // 如 "TECH", "ORG"
    ├── String extractionText         // 如 "Java", "Google"
    ├── String description
    └── List<String> attributes
```

### 3.3 EdgeType 枚举（结构化关系类型）

```java
public enum EdgeType {
    // 通用
    RELATED_TO, MENTIONS,
    // 结构
    PART_OF, IS_A, EXTENDS, IMPLEMENTS,
    // 依赖
    DEPENDS_ON, USED_BY, CREATED_BY,
    // 演进
    DERIVED_FROM, SUPERSEDES, COMPETES_WITH,
    // 冲突
    CONTRADICTS,
    // 相似
    SIMILAR_TO
}
```

### 3.4 AlignmentStatus 枚举

```java
public enum AlignmentStatus {
    EXACT,   // Tier 1: String.indexOf 精确匹配
    FUZZY,   // Tier 2: LCS 相似度 ≥ 0.75
    GREATER, // Tier 2 变体：匹配窗口比实体名长
    LESSER   // Tier 3: Token 级密度 ≥ 1/3
}
```

---

## 4. 核心组件设计

### 4.1 分块策略 — SlidingWindowChunker

**目标**：替代原始的 `\n\n` 分割，使用滑动窗口 + 句子边界感知 + 重叠区域。

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `maxChunkSize` | 8000 | 每块最大字符数 |
| `overlapSize` | 200 | 相邻块之间的重叠字符数 |

**算法**：
1. 使用正则 `[.!?]+\s+|[.!?]+$` 检测句子边界
2. 按句子累加，当加入下一个句子会超过 `maxChunkSize` 时，发射当前块
3. 下一块从当前块末尾前 `overlapSize` 个字符处开始（确保边界实体不丢失）
4. 如果单个句子超过 `maxChunkSize`，强制分割

**输出**：`List<TextChunk>`，每个 TextChunk 包含：
- `text`：块文本内容
- `startOffset` / `endOffset`：在原文中的字符位置
- `overlapStart` / `overlapEnd`：重叠区域位置

### 4.2 Few-shot 提示词 — PromptTemplate + EntityExampleService

**目标**：从已标注数据中动态构建 examples 注入 prompt，替代 zero-shot。

**流程**：
1. `EntityExampleService.loadExamplesAsExampleData(entityType)` 从 `entity_examples` 表加载标注数据
2. `PromptTemplate.render(inputText)` 将 examples 格式化为 `Example 1: Text: ... Extractions: ...` 形式
3. `OpenAiApiClient.buildFewShotPrompt(basePrompt, examples)` 将 few-shot 示例附加到系统 prompt

**Prompt 格式**：
```
{base_system_prompt}

Example 1:
Text: {example_text}
Extractions:
- TECH: Java (programming language) [OOP, statically-typed]
- ORG: Google (technology company)

Example 2:
Text: {example_text}
Extractions:
- TECH: Python

Text: {input_text}
Extractions:
```

### 4.3 Source Grounding — AlignmentResolver

**目标**：为每个实体/概念找到在原文中的精确字符位置（`startOffset/endOffset`）。

**三级对齐策略**（移植自 LangExtract 的 resolver.py）：

| 层级 | 策略 | 阈值 | 状态 |
|------|------|------|------|
| Tier 1 | `String.indexOf()` 精确匹配 | 完全匹配 | `EXACT` |
| Tier 2 | 滑动窗口 LCS（最长公共子序列）相似度 | ratio ≥ 0.75 | `FUZZY` |
| Tier 3 | Token 级密度匹配 | density ≥ 1/3 | `LESSER` |

**执行时机**：
- 如果 LLM 在 prompt 要求下已返回 `start_offset/end_offset`，直接使用（状态 = `EXACT`）
- 否则，调用 `AlignmentResolver.alignEntity(name, sourceText)` 进行三级回退对齐

### 4.4 结构化关系抽取 — RelationInfo

**目标**：替代 `List<String> relatedEntities`，提供有方向、有类型、有置信度的关系。

**关系类型**（映射到 `EdgeType` 枚举）：
- `DEPENDS_ON`：A 依赖 B
- `IS_A`：A 是 B 的一种
- `PART_OF`：A 是 B 的一部分
- `CREATED_BY`：A 由 B 创建
- `USED_BY`：A 被 B 使用
- `COMPETES_WITH`：A 与 B 竞争
- `IMPLEMENTS`：A 实现 B
- `EXTENDS`：A 继承/扩展 B
- `RELATED_TO`：通用关系
- `MENTIONS`：A 提及 B

**过滤规则**：
- Prompt 中要求 LLM 仅返回 confidence ≥ 0.5 的关系
- `PipelineService` 中 `relation.isConfident(0.5)` 二次校验
- `relation.hasValidType()` 确保关系类型非空

### 4.5 统一抽取 — unifiedExtract

**目标**：单次 LLM 调用同时抽取实体 + 概念 + 关系，减少不一致性。

**注意**：当前实现中，`extractEntities()` 已在 prompt 中包含关系抽取（实体提取时同步返回 relations），而 `unifiedExtract()` 作为更进一步的合并接口，在单次调用中返回完整的三元组。`extractConcepts()` 仍然是独立调用。

### 4.6 流水线集成 — PipelineService

**处理流程**：

```
RawDocument
  → [1] scoreDocument()           // AI 评分过滤
  → [2] extractEntities()         // 实体 + 关系提取（含 chunking + few-shot + alignment）
  → [3] extractConcepts()         // 概念提取（含 chunking + few-shot + alignment）
  → [4] matchKnowledgeGraph()     // 图谱匹配 + 边创建（含结构化关系）
  → [4.5] createComparisonNodes() // Karpathy Layer 2: COMPARISON 节点
  → [4.5] createQueryNodes()      // Karpathy Layer 2: QUERY 节点
  → [5] generatePage()            // 页面生成
  → [6] submitForApproval()       // 提交审批
  → [7] autoCrossLink()           // 交叉链接
  → [8] consistencyCheck()        // 一致性检查
```

**重试机制**：
- 每个步骤通过 `executeWithRetry()` 执行，默认最多 3 次
- 指数退避：1s → 2s → 4s
- 3 次失败后写入 `dead_letter_queue` 表，标记 `FAILED`

**Dead Letter Queue**：
- 表：`dead_letter_queue`
- 字段：`rawDocumentId`, `step`, `errorMessage`, `retryCount`, `maxRetries`, `status`
- 支持手动重试：`retryDeadLetter(id)`

### 4.7 一致性检查 — ConsistencyReport

检查项：
1. 页面标题非空
2. 页面内容 ≥ 50 字符
3. Slug 合法（仅含 `[a-z0-9-]`）
4. 所有实体名出现在页面内容中
5. 无悬空链接（链接目标页面存在）

输出：`ConsistencyReport { passed, issues[], entityCount, linkedPagesCount }`

---

## 5. 数据库变更

### 5.1 `entity_examples` 表（V8 迁移）

用于存储 few-shot 标注数据：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | UUID | 主键 |
| name | VARCHAR(255) | 示例名称 |
| description | TEXT | 描述 |
| example_text | TEXT | 示例文本 |
| extraction_data | TEXT | JSON 格式标注数据 |
| entity_type | VARCHAR(50) | 实体类型（PERSON/ORG/TECH 等） |
| deleted | BOOLEAN | 软删除标记 |
| created_at | TIMESTAMPTZ | 创建时间 |
| updated_at | TIMESTAMPTZ | 更新时间 |

### 5.2 `dead_letter_queue` 表

流水线失败重试队列：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | UUID | 主键 |
| raw_document_id | UUID | 关联原始文档 |
| step | VARCHAR(50) | 失败的步骤 |
| error_message | TEXT | 错误信息 |
| retry_count | INT | 已重试次数 |
| max_retries | INT | 最大重试次数 |
| status | VARCHAR(20) | PENDING/RETRYING/RESOLVED/FAILED |

---

## 6. 配置项

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| `ai.prompt.score` | （内置） | 评分 prompt 模板（空=使用默认） |
| `ai.prompt.entity` | （内置） | 实体提取 prompt 模板（空=使用默认） |
| `ai.prompt.concept` | （内置） | 概念提取 prompt 模板（空=使用默认） |
| `ai.prompt.unified` | （内置） | 统一提取 prompt 模板（空=使用默认） |
| `pipeline.max.retries` | 3 | 流水线步骤最大重试次数 |

Prompt 模板可通过 `application.yml` 的 `ai.prompt.*` 配置项覆盖，注入方式：
```java
@Value("${ai.prompt.entity:}") String entityPrompt
// 如果为空则使用内置默认 prompt
```

---

## 7. 与 LangExtract 对比（改进后）

| 维度 | 原始实现 | LangExtract | 本方案改进后 |
|------|---------|-------------|-------------|
| 提示词 | Zero-shot | Few-shot examples | ✅ Few-shot（动态 examples） |
| Source Grounding | ❌ 无 | Token-level alignment | ✅ 3-tier char-level alignment |
| 分块 | `\n\n` 无重叠 | 滑动窗口 + 多轮 | ✅ 滑动窗口 + 句子边界 + 200 overlap |
| 实体+概念+关系 | 分离调用 | 单次调用多类型 | ✅ 实体+关系合并；unifiedExtract 全合并 |
| 关系类型 | `List<String>` | 结构化类型 + 置信度 | ✅ `RelationInfo` + `EdgeType` 枚举 |
| 去重 | name 精确匹配 | 嵌入相似度 | ⚠️ 仍为 name 精确匹配（P2 待改进） |
| 多轮提取 | ❌ 无 | `extraction_passes=3` | ❌ 未实现（P2 待改进） |
| 可视化 | ❌ 无 | HTML 高亮 | ❌ 未规划 |

### 待改进项（P2）

1. **语义去重**：用 embedding 相似度替代 name 精确匹配，解决 "Apple"（公司）vs "apple"（水果）问题
2. **多轮提取**：对长文档执行 2-3 轮提取合并，提升召回率 10-20%
3. **实体消歧**：同名不同义项的区分机制

---

## 8. 文件清单

| 文件 | 说明 |
|------|------|
| `backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/api/AiApiClient.java` | AI 客户端接口（含 few-shot 重载） |
| `backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/api/OpenAiApiClient.java` | OpenAI 兼容实现（含 chunking/few-shot/alignment 集成） |
| `backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/dto/ExtractionResult.java` | 提取结果 DTO（EntityInfo + ConceptInfo，含 grounding 字段） |
| `backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/dto/UnifiedExtractionResult.java` | 统一提取结果 DTO |
| `backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/dto/RelationInfo.java` | 结构化关系 DTO |
| `backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/dto/ExampleData.java` | Few-shot 示例数据 DTO |
| `backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/chunking/SlidingWindowChunker.java` | 滑动窗口分块器 |
| `backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/chunking/TextChunk.java` | 文本块 DTO（含 offset/overlap） |
| `backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/resolver/AlignmentResolver.java` | 三级对齐解析器 |
| `backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/prompting/PromptTemplate.java` | Few-shot prompt 模板渲染 |
| `backend/llm-wiki-common/src/main/java/com/llmwiki/common/enums/EdgeType.java` | 关系类型枚举 |
| `backend/llm-wiki-common/src/main/java/com/llmwiki/common/enums/AlignmentStatus.java` | 对齐状态枚举 |
| `backend/llm-wiki-common/src/main/java/com/llmwiki/common/types/CharInterval.java` | 字符区间工具 |
| `backend/llm-wiki-service/src/main/java/com/llmwiki/service/pipeline/PipelineService.java` | 流水线编排（含重试/DLQ/一致性检查） |
| `backend/llm-wiki-service/src/main/java/com/llmwiki/service/pipeline/ConsistencyReport.java` | 一致性检查报告 |
| `backend/llm-wiki-service/src/main/java/com/llmwiki/service/example/EntityExampleService.java` | Few-shot 示例 CRUD 服务 |
| `backend/llm-wiki-domain/src/main/java/com/llmwiki/domain/pipeline/entity/DeadLetterQueue.java` | 死信队列实体 |
| `backend/llm-wiki-web/src/main/resources/db/migration/V8__create_entity_examples.sql` | Few-shot 示例表迁移 |
