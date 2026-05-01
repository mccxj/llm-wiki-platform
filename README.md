# LLM Wiki 自动化平台

基于 AI 的知识库自动化管理平台。从文档源同步内容，通过 AI 评分过滤、实体/概念提取、向量化，自动构建知识图谱，支持语义搜索和自然语言问答。

## 架构

```
┌─────────────┐     ┌──────────────┐     ┌─────────────────┐
│  Wiki 数据源  │────▶│  同步调度器   │────▶│  处理流水线       │
│  (API/爬虫)   │     │  (定时轮询)   │     │  评分→提取→向量化 │
└─────────────┘     └──────────────┘     └────────┬────────┘
                                                   │
                                                   ▼
┌─────────────┐     ┌──────────────┐     ┌─────────────────┐
│  前端 (React) │◀───│  REST API    │◀───│  知识图谱 (PG)   │
│  Antd + D3   │     │  (SpringBoot)│     │  节点+边+向量    │
└─────────────┘     └──────────────┘     └─────────────────┘
```

## 技术栈

- **后端**: Java 17 + Spring Boot 3.2 + Maven 多模块
- **数据库**: PostgreSQL 14 + pgvector (向量扩展)
- **AI**: OpenAI 兼容 API (评分、实体提取、Embedding、对话)
- **前端**: React 18 + TypeScript + Ant Design + D3.js
- **部署**: Docker Compose

## 快速启动

### 1. 环境要求

- Docker & Docker Compose
- Java 17 + Maven 3.9 (本地开发)
- Node.js 18+ (前端开发)

### 2. 配置 AI API

编辑 docker-compose.yml 中的环境变量：

```yaml
environment:
  AI_API_BASE_URL: http://your-llm-server/v1    # OpenAI兼容接口地址
  AI_API_KEY: your-api-key
  AI_MODEL: gpt-4o-mini                           # 对话模型
  EMBEDDING_MODEL: text-embedding-ada-002         # 向量模型
  EMBEDDING_DIMENSION: 1536
```

### 3. 启动服务

```bash
# 启动数据库 + 后端 + 前端
docker-compose up -d

# 查看日志
docker-compose logs -f backend
```

- 前端: http://localhost:3000
- 后端 API: http://localhost:8080/api
- API 文档: http://localhost:8080/api

## 模块结构

```
llm-wiki-platform/
├── backend/
│   ├── llm-wiki-common/        # 公共枚举、DTO
│   ├── llm-wiki-domain/        # 实体、Repository
│   ├── llm-wiki-adapter/       # AI API 客户端 (可替换实现)
│   ├── llm-wiki-service/       # 业务逻辑
│   └── llm-wiki-web/           # Controller、配置、启动类
├── frontend/                    # React 前端
├── docker-compose.yml
└── Dockerfile
```

## 处理流水线

1. **定时同步** — 轮询 Wiki 数据源，增量获取文档
2. **AI 评分** — 多维度评分（相关性/完整性/准确性/清晰度/结构），低于阈值跳过
3. **实体提取** — 识别文档中的人名、组织、技术、概念等
4. **概念提取** — 提取抽象主题和关联关系
5. **知识图谱** — 匹配/创建节点和边，生成向量嵌入
6. **页面生成** — 生成结构化 Markdown 页面
7. **交叉链接** — 自动关联相关页面
8. **一致性检查** — 基础质量校验
9. **审批** — 管理员审批后生效

## API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/pages | 页面列表 |
| GET | /api/pages/{id} | 页面详情 |
| POST | /api/pipeline/trigger/{docId} | 触发文档处理 |
| GET | /api/approvals | 审批列表 |
| POST | /api/approvals/{id}/approve | 审批通过 |
| POST | /api/approvals/{id}/reject | 审批拒绝 |
| GET | /api/search?q= | 语义搜索 |
| POST | /api/ask | 自然语言问答 |
| GET | /api/graph/nodes | 图谱节点 |
| GET | /api/graph/edges | 图谱边 |

## 许可证

MIT
