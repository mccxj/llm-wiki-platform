# E-6: 结构化关系类型

## 目标
增强关系抽取，使 LLM 输出预定义的结构化关系类型（带方向性和置信度）。

## 步骤

### 1. 增强 EdgeType 枚举
文件: `backend/llm-wiki-common/src/main/java/com/llmwiki/common/enums/EdgeType.java`

新增关系类型：
- DEPENDS_ON（依赖）
- IS_A（是一种）
- PART_OF（属于）
- CREATED_BY（由...创建）
- USED_BY（被...使用）
- COMPETES_WITH（竞争）
- IMPLEMENTS（实现）
- EXTENDS（扩展）

保留原有的 RELATED_TO, DERIVED_FROM, CONTRADICTS, SUPERSEDES, MENTIONS。

### 2. 新增 RelationInfo DTO
文件: `backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/dto/RelationInfo.java`

字段：
- sourceEntity (String)
- targetEntity (String)
- relationType (String) — 对应 EdgeType 名称
- confidence (Double, 0.0-1.0)

### 3. 修改 ExtractionResult
文件: `backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/dto/ExtractionResult.java`

新增字段：
- List<RelationInfo> relations

### 4. 修改 OpenAiApiClient 的 extractEntities prompt
文件: `backend/llm-wiki-adapter/src/main/java/com/llmwiki/adapter/api/OpenAiApiClient.java`

在实体提取 prompt 中增加关系抽取指令：
- 引导模型识别实体间的结构化关系
- 输出格式包含 relations 数组
- 每条关系包含 source, target, type, confidence

### 5. 修改 PipelineService
文件: `backend/llm-wiki-service/src/main/java/com/llmwiki/service/pipeline/PipelineService.java`

在 matchKnowledgeGraph 中：
- 遍历 relations 创建 edges
- 使用 relation type 映射到 EdgeType
- 使用 confidence 作为 edge weight
- 过滤低于置信度阈值的关系

### 6. 测试
- RelationInfo 单元测试
- EdgeType 映射测试
- PipelineService 关系处理测试
- OpenAiApiClient prompt 测试

## 验收标准
- [ ] EdgeType 枚举包含至少 10 种语义关系
- [ ] ExtractionResult 包含 relations 列表
- [ ] LLM prompt 引导输出结构化关系
- [ ] PipelineService 使用 relation type 和 confidence
- [ ] 所有测试通过
