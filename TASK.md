<<<<<<< HEAD
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

---

# E-4: Sliding Window Chunking

## GitHub Issue
#36 [E-4][P1] Replace naive chunking with sliding window strategy

## Problem
Current chunking splits on \n\n with no overlap. Entities at boundaries are truncated.

## LangExtract Reference
- langextract/chunking.py: TextChunk with token_interval tracking position in source
- langextract/core/tokenizer.py: Sentence boundary detection

## Implementation Plan

### 1. Create TextChunk class (llm-wiki-adapter)
```java
public class TextChunk {
    private String text;
    private int startOffset;  // char position in source
    private int endOffset;
    private int overlapStart; // overlap region start
    private int overlapEnd;   // overlap region end
}
```

### 2. Create SlidingWindowChunker (llm-wiki-adapter, new class)
- Config: maxChunkSize (default 8000), overlapSize (default 200)
- Split on sentence boundaries when possible
- Track position in source document
- Handle edge cases: single sentence > maxChunkSize

### 3. Update OpenAiApiClient
- Replace splitIntoChunks() with SlidingWindowChunker
- Track chunk positions for entity offset adjustment

### 4. Tests
- Chunking with overlap
- Sentence boundary detection
- Position tracking accuracy
- Edge cases: very long sentence, empty text

## Files to Create
- backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/chunking/TextChunk.java
- backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/chunking/SlidingWindowChunker.java

## Files to Modify
- backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/api/OpenAiApiClient.java
=======
# Task: E-5 Unified Extraction

## GitHub Issue
#37 [E-5][P1] Unified extraction — single LLM call for entities + concepts + relations

## Problem
Two separate LLM calls for entities/concepts causes inconsistent classification and 2x API cost.

## Implementation Plan

### 1. Add extractAll() to AiApiClient interface
```java
ExtractionResult extractAll(String content);
```

### 2. Update ExtractionResult
- Add relations field: List<RelationInfo>
- RelationInfo: sourceName, targetName, type, confidence

### 3. Update OpenAiApiClient
- Unified prompt requesting entities + concepts + relations in one JSON call
- New response schema:
```json
{
  "entities": [...],
  "concepts": [...],
  "relations": [{"source": "Java", "target": "OOP", "type": "implements"}]
}
```

### 4. Update PipelineService
- Use extractAll() instead of separate extractEntities() + extractConcepts()
- Process relations from unified result

### 5. Tests
- Unified prompt structure
- Relation parsing
- Backward compatibility with separate calls

## Files to Modify
- backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/api/AiApiClient.java
- backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/dto/ExtractionResult.java
- backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/api/OpenAiApiClient.java
- backend/llm-wiki-service/src/main/java/com/llmwiki/service/pipeline/PipelineService.java
>>>>>>> origin/master
