# E-7: 语义去重替代 name 精确匹配

## 目标
使用 embedding 向量相似度进行实体去重，替代当前的 name 精确匹配。

## 步骤

### 1. 数据库变更
文件: `backend/llm-wiki-infrastructure/src/main/resources/db/migration/V7__add_embedding_to_kg_nodes.sql`

为 kg_nodes 表添加 embedding 列：
```sql
ALTER TABLE kg_nodes ADD COLUMN embedding vector(1536);
CREATE INDEX idx_kg_nodes_embedding ON kg_nodes USING ivfflat (embedding vector_cosine_ops);
```

### 2. KgNode 实体添加 embedding 字段
文件: `backend/llm-wiki-domain/src/main/java/com/llmwiki/domain/graph/entity/KgNode.java`

新增字段：
- embedding (float[] 或 List<Float>)

### 3. KgNodeRepository 添加相似度查询
文件: `backend/llm-wiki-domain/src/main/java/com/llmwiki/domain/graph/repository/KgNodeRepository.java`

新增方法：
- findSimilarNodes(String name, float[] embedding, double threshold, int limit)
- 使用 pgvector 的 <=> 操作符计算余弦距离

### 4. EmbeddingService
新建: `backend/llm-wiki-service/src/main/java/com/llmwiki/service/graph/EmbeddingService.java`

方法：
- float[] computeEmbedding(String text) — 调用 OpenAI embedding API
- double cosineSimilarity(float[] a, float[] b)

### 5. 修改 PipelineService 的去重逻辑
文件: `backend/llm-wiki-service/src/main/java/com/llmwiki/service/pipeline/PipelineService.java`

在 matchKnowledgeGraph 中：
- 计算新实体的 embedding
- 调用 findSimilarNodes 查找相似实体
- 相似度 >= 0.85：合并（保留更丰富的描述）
- 相似度 0.70-0.85：标记为"待合并候选"
- 相似度 < 0.70：创建新实体

### 6. 配置
文件: `backend/llm-wiki-web/src/main/resources/application.yml`

新增配置：
```yaml
llm:
  wiki:
    dedup:
      similarity-threshold: 0.85
      warn-threshold: 0.70
```

### 7. 测试
- EmbeddingService 单元测试
- 相似度计算测试
- PipelineService 去重逻辑测试（精确匹配、模糊匹配、边界值）
- 使用 H2 的 text 类型模拟 embedding

## 验收标准
- [ ] kg_nodes 表有 embedding 列
- [ ] EmbeddingService 能计算文本 embedding
- [ ] PipelineService 使用相似度去重
- [ ] 可配置相似度阈值
- [ ] 所有测试通过
