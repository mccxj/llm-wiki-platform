# LLM Wiki 自动化平台 PRD

> 版本：v1.0 | 日期：2026-05-01 | 状态：待评审

---

## 1. 项目概述

### 1.1 背景

团队现有的 Wiki 平台积累了大量文档（数千页面），但这些文档彼此孤立、缺乏交叉引用、难以搜索和维护。基于 Karpathy 的 LLM Wiki 理念，构建一个自动化知识管理平台，将孤立文档转化为互联的知识图谱，提供语义搜索和自然语言问答能力。

### 1.2 目标

- 自动从现有 Wiki 平台同步文档，增量处理
- 通过 AI 评分过滤低价值内容，只处理有意义的文档
- 自动提取实体和概念，构建结构化知识图谱
- 提供语义搜索和自然语言问答
- 提供交互式知识图谱可视化
- 关键变更需管理员审批后生效

### 1.3 用户角色

| 角色 | 职责 |
|---|---|
| 普通用户 | 浏览知识库、搜索、问答、查看图谱 |
| 管理员 | 审批页面变更、管理同步配置、查看系统状态 |

### 1.4 核心约束

- **技术栈**：后端 Java，数据库 MariaDB 11.8+（原生 VECTOR 支持），Docker 部署
- **部署环境**：内网，Docker Compose
- **AI 接口**：OpenAI 兼容 API（由接入方提供 base URL 和 API key）
- **数据库统一**：所有数据（结构化数据 + 向量 + 关系）均存储在 PostgreSQL 中，不引入额外数据库组件
- **外部依赖隔离**：AI API、旧 Wiki 接入等非数据库依赖，通过抽象接口隔离，便于替换和测试

---

## 2. 功能需求

### 2.1 文档同步模块

#### 2.1.1 数据源接入（适配器模式）

设计通用的数据源接入接口 `WikiSourceAdapter`，由接入方实现对接逻辑。

```
接口定义：
- fetchChanges(since) → List<RawDocument>   // 获取增量变更
- fetchPage(id) → RawDocument                // 获取单个页面
- testConnection() → boolean                 // 测试连接
```

接入方需将页面内容统一转换为 Markdown 格式（富文本→Markdown 转换由适配器负责）。

#### 2.1.2 定时轮询同步

- 通过定时任务（Cron 表达式可配置）轮询数据源
- 基于上次同步时间戳做增量拉取
- 同步记录写入 `sync_log` 表，包含同步时间、获取数量、处理数量、失败数量
- 支持手动触发同步

#### 2.1.3 内容去重与漂移检测

- 对原始内容计算 SHA-256 哈希
- 哈希未变化则跳过处理
- 哈希变化则标记为"已更新"，进入处理流水线

### 2.2 AI 评分模块

#### 2.2.1 评分流程

文档进入流水线后，首先调用大模型 API 进行评分。低于配置阈值的文档跳过后续处理，记录跳过的原因。

#### 2.2.2 评分维度

通过一个精心设计的 Prompt，让大模型从以下维度评分（每项 1-10 分），返回综合加权分：

| 维度 | 权重 | 说明 |
|---|---|---|
| 信息密度 | 30% | 文档是否包含实质性信息，而非空泛/模板化内容 |
| 实体丰富度 | 25% | 是否包含可识别的实体（术语、概念、产品、人物等） |
| 知识独立性 | 20% | 文档自身是否能独立理解，不依赖外部上下文 |
| 结构完整性 | 15% | 是否有合理的文档结构（标题、分段、列表等） |
| 时效相关性 | 10% | 内容是否有时效价值，是否明显过时 |

#### 2.2.3 评分 Prompt 设计

```
你是一个文档质量评估专家。请从以下5个维度对文档进行评分（每项1-10分），
并给出综合评分（加权计算）和简短理由。

评分维度及权重：
1. 信息密度（30%）：文档是否包含实质性信息
2. 实体丰富度（25%）：是否包含可识别的实体和概念
3. 知识独立性（20%）：文档是否能独立理解
4. 结构完整性（15%）：文档结构是否合理
5. 时效相关性（10%）：内容是否有时效价值

请以 JSON 格式返回：
{
  "scores": {
    "information_density": <1-10>,
    "entity_richness": <1-10>,
    "knowledge_independence": <1-10>,
    "structure_integrity": <1-10>,
    "timeliness": <1-10>
  },
  "overall_score": <加权综合分, 1-10>,
  "reason": "<一句话理由>",
  "key_entities": ["实体1", "实体2", ...],
  "suggested_tags": ["标签1", "标签2", ...]
}

文档内容：
---
{document_content}
---
```

#### 2.2.4 阈值配置

- 评分阈值可在管理后台配置（默认 5.0/10）
- 支持按维度设置最低分（如信息密度不得低于 3 分）
- 评分结果持久化存储，便于后续分析和调优

### 2.3 文档处理流水线

```
原始文档
  ↓
[1] 文本提取与清洗（Markdown 标准化）
  ↓
[2] AI 评分过滤（低于阈值 → 跳过并记录）
  ↓
[3] 实体识别与提取（调用 LLM）
  ↓
[4] 概念提取与分类（调用 LLM）
  ↓
[5] 匹配知识图谱（匹配已有实体/概念 或 创建新节点）
  ↓
[6] 生成结构化页面（Entity / Concept / Comparison）
  ↓
[7] 自动建立交叉链接（[[wikilinks]]）
  ↓
[8] 一致性检查（矛盾检测、过时检测）
  ↓
[9] 写入待审批队列
  ↓
[10] 管理员审批 → 生效入库
```

每个步骤的处理结果写入 `processing_log` 表，支持追踪和排查。

### 2.4 审批模块

#### 2.4.1 审批范围

以下内容变更需要审批：
- 新页面的创建
- 已有页面的内容更新
- 页面间的交叉链接变更
- 页面删除/归档操作

#### 2.4.2 审批流程

1. 系统生成变更摘要（新增/修改/删除了什么），附带 diff 对比
2. 管理员在 Web UI 的审批面板中查看
3. 管理员可选择：**通过** / **拒绝** / **修改后通过**
4. 审批通过后，变更写入正式库
5. 审批结果记录到 `approval_log` 表

#### 2.4.3 审批面板功能

- 待审批列表（按时间排序，支持筛选）
- 变更详情页（展示 before/after diff）
- 批量审批（全部通过/拒绝）
- 审批历史查询

### 2.5 知识图谱模块

#### 2.5.1 存储设计

完全基于 PostgreSQL：

| 表 | 用途 |
|---|---|
| `kg_nodes` | 知识图谱节点（实体、概念、页面） |
| `kg_edges` | 节点间关系（边） |
| `kg_vectors` | 节点向量（使用 MariaDB VECTOR） |
| `kg_node_tags` | 节点标签关联 |

#### 2.5.2 节点类型

| 类型 | 说明 |
|---|---|
| `ENTITY` | 具体实体（人、组织、产品、技术等） |
| `CONCEPT` | 抽象概念/主题 |
| `COMPARISON` | 对比分析页面 |
| `RAW_SOURCE` | 原始文档来源 |
| `QUERY` | 有价值的查询结果 |

#### 2.5.3 边类型

| 类型 | 说明 |
|---|---|
| `RELATED_TO` | 相关关系 |
| `PART_OF` | 从属关系 |
| `DERIVED_FROM` | 衍生自（页面源自某文档） |
| `CONTRADICTS` | 矛盾关系 |
| `SUPERSEDES` | 替代/更新关系 |
| `MENTIONS` | 提及关系 |

#### 2.5.4 向量化策略

- **文档级向量**：整篇文档的 embedding，用于文档搜索
- **实体/概念级向量**：每个实体/概念的 embedding，用于实体匹配和语义问答
- 向量维度由 embedding 模型决定（如 text-embedding-ada-002 为 1536 维）
- 使用 MariaDB 11.8+ 原生 `VECTOR` 类型和 `VEC_DISTANCE()` 函数加速检索

### 2.6 搜索模块

#### 2.6.1 语义搜索

- 用户输入查询文本 → 生成查询向量 → MariaDB VEC_DISTANCE() 近似最近邻检索
- 支持按节点类型过滤（只搜实体/只搜概念等）
- 返回结果附带相关度分数和摘要片段

#### 2.6.2 探索式搜索

- 支持按标签浏览
- 支持按关系类型浏览（"查看与 X 相关的所有实体"）
- 搜索结果高亮匹配片段

### 2.7 自然语言问答模块

#### 2.7.1 问答流程

```
用户问题
  ↓
[1] 问题理解（意图识别 + 关键实体提取）
  ↓
[2] 向量检索（找到相关知识库内容）
  ↓
[3] 知识库内回答（基于检索内容生成答案）
  ↓  （如果知识库内容不足）
[4] 回退到大模型（标注"以下内容由 AI 生成，未在知识库中找到确切依据"）
  ↓
[5] 返回答案 + 来源标注
```

#### 2.7.2 来源标注规则

- 知识库内回答：标注引用的页面名称和链接
- 回退大模型：明确标注"AI 生成，知识库未覆盖"
- 混合回答：分别标注哪些部分来自知识库，哪些来自 AI 推理

### 2.8 知识图谱可视化模块

#### 2.8.1 图谱展示

- 节点-边图布局（力导向图）
- 节点按类型区分颜色和图标
- 边的粗细表示关系强度
- 支持缩放、拖拽、平移

#### 2.8.2 交互功能

- 点击节点 → 展开节点详情面板（基本信息、关联页面、关联实体）
- 双击节点 → 展开/收起邻居节点
- 搜索框 → 快速定位节点
- 过滤器 → 按节点类型/标签/关系类型过滤显示
- 路径查询 → 输入两个节点，展示最短路径

#### 2.8.3 技术选型

- 前端框架：React + Ant Design
- 图谱可视化：D3.js 或 Cytoscape.js（择一）

### 2.9 维护模块

系统自动运行的维护任务（定时执行）：

| 功能 | 说明 | 频率 |
|---|---|---|
| 孤儿检测 | 查找没有入链的页面 | 每日 |
| 过时检测 | 查找长时间未更新的页面 | 每周 |
| 矛盾检测 | 查找同一主题下有冲突观点的页面 | 每周 |
| 合并建议 | 查找内容高度相似的页面，建议合并 | 每月 |
| 拆分建议 | 查找超过 200 行的页面，建议拆分 | 每月 |
| 索引一致性 | 检查所有页面是否在索引中有记录 | 每日 |

检测结果生成报告，推送给管理员。

---

## 3. 架构设计

### 3.1 系统架构图

```
┌─────────────────────────────────────────────────────────┐
│                      Web 前端                            │
│  (搜索/问答/图谱可视化/审批面板/管理后台)                    │
└──────────────────────┬──────────────────────────────────┘
                       │ REST API
┌──────────────────────┴──────────────────────────────────┐
│                    Java 后端                              │
│                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ 同步服务      │  │ 处理流水线    │  │ 搜索服务      │  │
│  │ (Scheduler)   │  │ (Pipeline)   │  │ (Search)     │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
│         │                 │                  │           │
│  ┌──────┴───────┐  ┌──────┴───────┐  ┌──────┴───────┐  │
│  │ 数据源适配器  │  │ AI 评分服务   │  │ 问答服务      │  │
│  │ (Adapter)     │  │ (Scoring)    │  │ (QA)         │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
│         │                 │                  │           │
│  ┌──────┴─────────────────┴──────────────────┴───────┐  │
│  │              服务层 (Service Layer)                 │  │
│  └──────────────────────┬────────────────────────────┘  │
│                         │                                │
│  ┌──────────────────────┴────────────────────────────┐  │
│  │           抽象接口层 (Abstraction Layer)            │  │
│  │  WikiSourceAdapter | AiApiClient | VectorStore     │  │
│  └──────────────────────┬────────────────────────────┘  │
└─────────────────────────┼───────────────────────────────┘
                          │
┌─────────────────────────┴───────────────────────────────┐
│                    PostgreSQL                             │
│  ┌─────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐  │
│  │ 业务数据  │ │ 知识图谱  │ │ 向量索引  │ │ 日志/审计   │  │
│  └─────────┘ └──────────┘ └──────────┘ └────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### 3.2 分层设计

| 层级 | 职责 |
|---|---|
| Controller 层 | REST API 入口，参数校验，权限控制 |
| Service 层 | 业务逻辑编排 |
| Abstraction 层 | 外部依赖的抽象接口（AI API、数据源、向量库） |
| Adapter 层 | 抽象接口的具体实现 |
| Repository 层 | 数据访问（PostgreSQL） |

### 3.3 外部依赖隔离

所有非数据库的外部依赖通过抽象接口隔离：

| 抽象接口 | 职责 | 当前实现 | 可替换为 |
|---|---|---|---|
| `AiApiClient` | 大模型调用（评分、实体提取、问答） | OpenAI 兼容 API | 任何兼容接口的 LLM |
| `WikiSourceAdapter` | 旧 Wiki 数据接入 | 待接入方实现 | 任何 Wiki/文档源 |
| `EmbeddingClient` | 文本向量化 | OpenAI Embedding | 本地 embedding 模型 |

---

## 4. 数据模型设计

### 4.1 核心实体表

#### `pages` — 页面表

| 字段 | 类型 | 说明 |
|---|---|---|
| id | UUID | 主键 |
| slug | VARCHAR(255) | URL 友好的唯一标识 |
| title | VARCHAR(500) | 页面标题 |
| content | TEXT | 页面内容（Markdown） |
| page_type | ENUM | ENTITY / CONCEPT / COMPARISON / QUERY / RAW_SOURCE |
| status | ENUM | PENDING_APPROVAL / APPROVED / REJECTED / ARCHIVED |
| confidence | ENUM | HIGH / MEDIUM / LOW |
 | contested | BOOLEAN | 是否存在争议 |
| ai_score | DECIMAL(3,1) | AI 综合评分 |
| ai_score_detail | JSONB | 各维度评分详情 |
| source_id | VARCHAR(255) | 原始文档 ID |
| created_at | TIMESTAMPTZ | 创建时间 |
| updated_at | TIMESTAMPTZ | 更新时间 |
| approved_by | UUID | 审批人 |
| approved_at | TIMESTAMPTZ | 审批时间 |

#### `page_tags` — 页面标签

| 字段 | 类型 | 说明 |
|---|---|---|
| page_id | UUID | 关联页面 |
| tag | VARCHAR(100) | 标签名 |

#### `page_links` — 页面交叉链接

| 字段 | 类型 | 说明 |
|---|---|---|
| source_page_id | UUID | 源页面 |
| target_page_id | UUID | 目标页面 |
| link_type | ENUM | RELATED_TO / PART_OF / DERIVED_FROM / CONTRADICTS / SUPERSEDES / MENTIONS |

#### `page_sources` — 页面来源

| 字段 | 类型 | 说明 |
|---|---|---|
| page_id | UUID | 关联页面 |
| raw_source_id | UUID | 关联原始文档 |
| source_excerpt | TEXT | 来源片段 |

### 4.2 知识图谱表

#### `kg_nodes` — 图谱节点

| 字段 | 类型 | 说明 |
|---|---|---|
| id | UUID | 主键 |
| name | VARCHAR(500) | 节点名称 |
| node_type | ENUM | ENTITY / CONCEPT / COMPARISON / RAW_SOURCE / QUERY |
| description | TEXT | 节点描述 |
| page_id | UUID | 关联页面（可选） |
| created_at | TIMESTAMPTZ | 创建时间 |
| updated_at | TIMESTAMPTZ | 更新时间 |

#### `kg_edges` — 图谱边

| 字段 | 类型 | 说明 |
|---|---|---|
| id | UUID | 主键 |
| source_node_id | UUID | 源节点 |
| target_node_id | UUID | 目标节点 |
| edge_type | ENUM | RELATED_TO / PART_OF / DERIVED_FROM / CONTRADICTS / SUPERSEDES / MENTIONS |
| weight | DECIMAL(3,2) | 关系强度 0.00-1.00 |
| created_at | TIMESTAMPTZ | 创建时间 |

#### `kg_vectors` — 节点向量

| 字段 | 类型 | 说明 |
|---|---|---|
| node_id | UUID | 关联节点 |
| vector | VECTOR(N) | 向量值（MariaDB 原生 VECTOR） |
| model | VARCHAR(100) | 使用的 embedding 模型 |
| created_at | TIMESTAMPTZ | 创建时间 |

### 4.3 审批表

#### `approval_queue` — 审批队列

| 字段 | 类型 | 说明 |
|---|---|---|
| id | UUID | 主键 |
| entity_type | VARCHAR(50) | 变更对象类型（PAGE / LINK / NODE 等） |
| entity_id | UUID | 变更对象 ID |
| action | ENUM | CREATE / UPDATE / DELETE |
| before_value | JSONB | 变更前内容 |
| after_value | JSONB | 变更后内容 |
| summary | TEXT | 变更摘要 |
| status | ENUM | PENDING / APPROVED / REJECTED |
| reviewer_id | UUID | 审批人 |
| reviewed_at | TIMESTAMPTZ | 审批时间 |
| created_at | TIMESTAMPTZ | 创建时间 |

### 4.4 同步与日志表

#### `sync_log` — 同步日志

| 字段 | 类型 | 说明 |
|---|---|---|
| id | UUID | 主键 |
| started_at | TIMESTAMPTZ | 开始时间 |
| finished_at | TIMESTAMPTZ | 结束时间 |
| fetched_count | INT | 获取文档数 |
| processed_count | INT | 处理成功数 |
| skipped_count | INT | 跳过数（评分不足） |
| failed_count | INT | 失败数 |
| status | ENUM | RUNNING / SUCCESS / PARTIAL / FAILED |

#### `processing_log` — 处理流水线日志

| 字段 | 类型 | 说明 |
|---|---|---|
| id | UUID | 主键 |
| raw_document_id | VARCHAR(255) | 原始文档 ID |
| step | VARCHAR(50) | 流水线步骤 |
| status | ENUM | SUCCESS / FAILED / SKIPPED |
| detail | JSONB | 详细结果 |
| created_at | TIMESTAMPTZ | 创建时间 |

#### `raw_documents` — 原始文档

| 字段 | 类型 | 说明 |
|---|---|---|
| id | UUID | 主键 |
| source_id | VARCHAR(255) | 来源系统的文档 ID |
| title | VARCHAR(500) | 标题 |
| content | TEXT | 内容（Markdown） |
| content_hash | VARCHAR(64) | SHA-256 哈希 |
| source_url | TEXT | 原始 URL |
| ingested_at | TIMESTAMPTZ | 首次入库时间 |
| last_checked_at | TIMESTAMPTZ | 最后检查时间 |

#### `wiki_sources` — 数据源配置

| 字段 | 类型 | 说明 |
|---|---|---|
| id | UUID | 主键 |
| name | VARCHAR(255) | 数据源名称 |
| adapter_class | VARCHAR(255) | 适配器类名 |
| config | JSONB | 适配器配置（连接参数等） |
| sync_cron | VARCHAR(100) | 同步 Cron 表达式 |
| last_sync_at | TIMESTAMPTZ | 最后同步时间 |
| enabled | BOOLEAN | 是否启用 |

#### `system_config` — 系统配置

| 字段 | 类型 | 说明 |
|---|---|---|
| key | VARCHAR(100) | 配置键 |
| value | TEXT | 配置值 |
| description | VARCHAR(500) | 说明 |

配置项包括：
- `ai_score_threshold`：AI 评分阈值（默认 5.0）
- `ai_api_base_url`：AI API 地址
- `ai_api_key`：AI API 密钥
- `ai_model`：使用的模型名
- `embedding_model`：Embedding 模型名
- `sync_batch_size`：同步批处理大小

---

## 5. 接口设计

### 5.1 同步接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/sync/trigger` | 手动触发同步 |
| GET | `/api/sync/status` | 查看同步状态 |
| GET | `/api/sync/logs` | 查看同步日志 |

### 5.2 审批接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/approvals/pending` | 获取待审批列表 |
| GET | `/api/approvals/{id}` | 获取审批详情（含 diff） |
| POST | `/api/approvals/{id}/approve` | 通过审批 |
| POST | `/api/approvals/{id}/reject` | 拒绝审批 |
| POST | `/api/approvals/batch` | 批量审批 |
| GET | `/api/approvals/history` | 审批历史 |

### 5.3 搜索接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/search` | 语义搜索 |
| GET | `/api/search/suggest` | 搜索建议 |

请求体：
```json
{
  "query": "搜索文本",
  "types": ["ENTITY", "CONCEPT"],
  "tags": ["标签1"],
  "limit": 20,
  "offset": 0
}
```

### 5.4 问答接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/qa/ask` | 自然语言问答 |

请求体：
```json
{
  "question": "问题内容",
  "session_id": "会话 ID（可选，用于多轮对话）"
}
```

响应体：
```json
{
  "answer": "回答内容",
  "sources": [
    {
      "page_id": "页面 ID",
      "page_title": "页面标题",
      "snippet": "引用片段"
    }
  ],
  "ai_fallen_back": false,
  "confidence": 0.85
}
```

### 5.5 知识图谱接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/graph` | 获取图谱数据（支持过滤） |
| GET | `/api/graph/nodes/{id}` | 获取节点详情 |
| GET | `/api/graph/nodes/{id}/neighbors` | 获取邻居节点 |
| GET | `/api/graph/path` | 查询两节点间路径 |
| GET | `/api/graph/stats` | 图谱统计信息 |

### 5.6 页面接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/pages` | 页面列表 |
| GET | `/api/pages/{id}` | 页面详情 |
| GET | `/api/pages/{id}/links` | 页面的交叉链接 |
| GET | `/api/pages/{id}/history` | 页面变更历史 |

### 5.7 管理接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/admin/config` | 获取系统配置 |
| PUT | `/api/admin/config` | 更新系统配置 |
| POST | `/api/admin/maintenance/orphans` | 手动触发孤儿检测 |
| POST | `/api/admin/maintenance/stale` | 手动触发过时检测 |
| POST | `/api/admin/maintenance/contradictions` | 手动触发矛盾检测 |
| GET | `/api/admin/reports/maintenance` | 获取维护报告 |

---

## 6. 非功能需求

### 6.1 性能要求

| 指标 | 要求 |
|---|---|
| 语义搜索响应时间 | < 500ms |
| 问答响应时间 | < 5s（知识库内）/ < 10s（回退大模型）|
| 图谱加载（首次） | < 3s（1000 节点以内）|
| 同步处理吞吐 | > 50 文档/分钟 |
| 并发用户 | 支持 50 并发 |

### 6.2 可靠性

- 流水线各步骤失败可重试，重试 3 次后进入死信队列
- 审批队列数据不丢失
- 数据库每日自动备份

### 6.3 安全性

- 管理员操作需要认证（JWT Token）
- API Key 加密存储
- 操作审计日志完整记录

### 6.4 可扩展性

- 数据源适配器可插拔
- AI 实现可替换（通过 `AiApiClient` 接口）
- 流水线步骤可配置（通过配置表控制启用/跳过某些步骤）

---

## 7. 部署方案

### 7.1 Docker Compose 编排

```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mariadb://db:3306/llmwiki
      - AI_API_BASE_URL=${AI_API_BASE_URL}
      - AI_API_KEY=${AI_API_KEY}
    depends_on:
      db:
        condition: service_healthy

  db:
    image: mariadb:11.8
    environment:
      - MARIADB_DATABASE=llmwiki
      - MARIADB_USER=llmwiki
      - MARIADB_PASSWORD=${DB_PASSWORD}
      - MARIADB_ROOT_PASSWORD=${DB_PASSWORD}
    volumes:
      - mariadb_data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    healthcheck:
      test: ["CMD", "healthcheck.sh", "--connect", "--innodb_initialized"]
      interval: 10s
      timeout: 5s
      retries: 5

  frontend:
    build: ./frontend
    ports:
      - "3000:80"
    depends_on:
      - app

volumes:
  mariadb_data:
```

### 7.2 环境变量

| 变量 | 说明 | 必需 |
|---|---|---|
| `AI_API_BASE_URL` | OpenAI 兼容 API 地址 | 是 |
| `AI_API_KEY` | API 密钥 | 是 |
| `AI_MODEL` | 模型名称（默认 gpt-4o-mini） | 否 |
| `EMBEDDING_MODEL` | Embedding 模型（默认 text-embedding-ada-002） | 否 |
| `DB_PASSWORD` | 数据库密码 | 是 |
| `AI_SCORE_THRESHOLD` | 评分阈值（默认 5.0） | 否 |

### 7.3 目录结构

```
llm-wiki-platform/
├── docker-compose.yml
├── Dockerfile
├── init.sql                    # 数据库初始化脚本
├── backend/                    # Java 后端
│   ├── src/main/java/
│   │   ├── controller/         # REST API 控制器
│   │   ├── service/            # 业务逻辑
│   │   │   ├── sync/           # 同步服务
│   │   │   ├── pipeline/       # 处理流水线
│   │   │   ├── scoring/        # AI 评分
│   │   │   ├── search/         # 搜索服务
│   │   │   ├── qa/             # 问答服务
│   │   │   ├── graph/          # 知识图谱服务
│   │   │   ├── approval/       # 审批服务
│   │   │   └── maintenance/    # 维护服务
│   │   ├── adapter/            # 外部依赖适配器
│   │   │   ├── ai/             # AI API 适配器
│   │   │   ├── wiki/           # Wiki 数据源适配器
│   │   │   └── embedding/      # Embedding 适配器
│   │   ├── repository/         # 数据访问层
│   │   ├── model/              # 数据模型
│   │   └── config/             # 配置类
│   └── pom.xml
├── frontend/                   # Web 前端
│   ├── src/
│   │   ├── views/              # 页面视图
│   │   │   ├── search/         # 搜索页
│   │   │   ├── qa/             # 问答页
│   │   │   ├── graph/          # 图谱页
│   │   │   ├── approval/       # 审批页
│   │   │   └── admin/          # 管理后台
│   │   ├── components/         # 通用组件
│   │   └── api/                # API 调用封装
│   └── package.json
└── README.md
```

---

## 8. Karpathy 三层架构映射

将 LLM Wiki 的三层架构映射到本系统的存储和逻辑中：

| Karpathy 层 | 本系统对应 | 说明 |
|---|---|---|
| Layer 1: `raw/` | `raw_documents` 表 | 不可变原始文档，存 SHA-256 哈希用于漂移检测 |
| Layer 2: `entities/` | `pages` (type=ENTITY) + `kg_nodes` | 实体页面 + 图谱节点 |
| Layer 2: `concepts/` | `pages` (type=CONCEPT) + `kg_nodes` | 概念页面 + 图谱节点 |
| Layer 2: `comparisons/` | `pages` (type=COMPARISON) | 对比分析页面 |
| Layer 2: `queries/` | `pages` (type=QUERY) | 有价值的查询结果 |
| Layer 3: `SCHEMA.md` | `system_config` + `page_tags` 标签体系 | 系统配置和标签分类体系 |
| `index.md` | `pages` 表的索引查询 + 搜索服务 | 内容目录 |
| `log.md` | `sync_log` + `processing_log` + `approval_log` | 多维度日志 |

---

## 附录 A：AI 评分维度权重调优建议

初始权重基于通用知识库场景设定，实际使用中建议：

- **技术文档库**：提高"实体丰富度"权重（技术术语是关键）
- **新闻资讯库**：提高"时效相关性"权重
- **研究论文库**：提高"信息密度"权重
- **内部流程文档**：提高"知识独立性"权重

权重可通过 `system_config` 表动态调整，无需重启服务。

---

## 附录 B：术语表

| 术语 | 说明 |
|---|---|
| Wikilink | `[[页面名]]` 格式的页面间交叉引用链接 |
| Entity | 知识图谱中的具体实体节点（人、组织、产品等）|
| Concept | 知识图谱中的抽象概念节点 |
| Raw Source | 从原始 Wiki 同步过来的不可变源文档 |
| Drift Detection | 通过 SHA-256 哈希检测原始文档是否发生变化 |
| Orphan Page | 没有其他页面链接到的孤立页面 |
| Contested Page | 存在争议或矛盾的页面 |
