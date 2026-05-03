package com.llmwiki.service.pipeline;

import com.llmwiki.adapter.api.AiApiClient;
import com.llmwiki.adapter.api.EmbeddingClient;
import com.llmwiki.adapter.dto.ExampleData;
import com.llmwiki.adapter.dto.ExtractionResult;
import com.llmwiki.adapter.dto.ExtractionResult.ConceptInfo;
import com.llmwiki.adapter.dto.ExtractionResult.EntityInfo;
import com.llmwiki.adapter.dto.RelationInfo;
import com.llmwiki.adapter.dto.ScoreResult;
import com.llmwiki.common.enums.*;
import com.llmwiki.domain.approval.entity.ApprovalQueue;
import com.llmwiki.domain.approval.repository.ApprovalQueueRepository;
import com.llmwiki.domain.graph.entity.KgEdge;
import com.llmwiki.domain.graph.entity.KgNode;
import com.llmwiki.domain.graph.entity.KgVector;
import com.llmwiki.domain.graph.repository.KgEdgeRepository;
import com.llmwiki.domain.graph.repository.KgNodeRepository;
import com.llmwiki.domain.graph.repository.KgVectorRepository;
import com.llmwiki.domain.page.entity.Page;
import com.llmwiki.domain.page.entity.PageLink;
import com.llmwiki.domain.page.entity.PageTag;
import com.llmwiki.domain.page.repository.PageLinkRepository;
import com.llmwiki.domain.page.repository.PageRepository;
import com.llmwiki.domain.page.repository.PageTagRepository;
import com.llmwiki.domain.pipeline.entity.DeadLetterQueue;
import com.llmwiki.domain.pipeline.repository.DeadLetterQueueRepository;
import com.llmwiki.domain.processing.entity.ProcessingLog;
import com.llmwiki.domain.processing.repository.ProcessingLogRepository;
import com.llmwiki.domain.sync.entity.RawDocument;
import com.llmwiki.domain.sync.repository.RawDocumentRepository;
import com.llmwiki.domain.config.repository.SystemConfigRepository;
import com.llmwiki.service.example.EntityExampleService;
import com.llmwiki.service.scoring.ScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PipelineService {

    private static final int DEFAULT_MAX_RETRIES = 3;

    private final RawDocumentRepository rawDocRepo;
    private final ProcessingLogRepository procLogRepo;
    private final KgNodeRepository kgNodeRepo;
    private final KgEdgeRepository kgEdgeRepo;
    private final KgVectorRepository kgVectorRepo;
    private final PageRepository pageRepo;
    private final PageLinkRepository pageLinkRepo;
    private final PageTagRepository pageTagRepo;
    private final ApprovalQueueRepository approvalQueueRepo;
    private final DeadLetterQueueRepository deadLetterQueueRepo;
    private final SystemConfigRepository configRepo;

    private final AiApiClient aiClient;
    private final EmbeddingClient embeddingClient;
    private final ScoringService scoringService;
    private final EntityExampleService entityExampleService;

    @Transactional
    public void processDocument(UUID rawDocId) {
        RawDocument doc = rawDocRepo.findById(rawDocId)
                .orElseThrow(() -> new IllegalArgumentException("Raw document not found: " + rawDocId));

        log.info("Starting pipeline for document: {} ({})", doc.getTitle(), doc.getId());

        // Step 1: Score
        ScoreResult scoreResult = executeWithRetry(rawDocId, "SCORE", () -> scoreDocument(doc));
        if (scoreResult == null) {
            return;
        }
        if (!scoringService.passesThreshold(scoreResult)) {
            BigDecimal threshold = scoringService.getThreshold();
            log.info("Document {} score {} below threshold {}, skipping", doc.getId(), scoreResult.getOverallScore(), threshold);
            saveStepLog(rawDocId, "SCORE", StepStatus.SKIPPED, "Score " + scoreResult.getOverallScore() + " below threshold " + threshold);
            return;
        }
        saveStepLog(rawDocId, "SCORE", StepStatus.SUCCESS, scoreResult.getReason());

        // Step 2: Entity Extraction
        ExtractionResult entities = executeWithRetry(rawDocId, "ENTITY_EXTRACTION", () -> extractEntities(doc));
        if (entities == null) return;
        saveStepLog(rawDocId, "ENTITY_EXTRACTION", StepStatus.SUCCESS, "Extracted " + entities.getEntities().size() + " entities");

        // Step 3: Concept Extraction
        ExtractionResult concepts = executeWithRetry(rawDocId, "CONCEPT_EXTRACTION", () -> extractConcepts(doc));
        if (concepts == null) return;
        saveStepLog(rawDocId, "CONCEPT_EXTRACTION", StepStatus.SUCCESS, "Extracted " + concepts.getConcepts().size() + " concepts");

        // Step 4: Graph Matching
        List<KgNode> matchedNodes = executeWithRetry(rawDocId, "GRAPH_MATCHING", () -> matchKnowledgeGraph(entities, concepts));
        if (matchedNodes == null) return;
        saveStepLog(rawDocId, "GRAPH_MATCHING", StepStatus.SUCCESS, "Matched " + matchedNodes.size() + " graph nodes");

        // Step 4.5: Create COMPARISON and QUERY nodes (Karpathy 3-layer)
        Integer comparisons = executeWithRetry(rawDocId, "COMPARISON_CREATION", () -> createComparisonNodes(entities));
        saveStepLog(rawDocId, "COMPARISON_CREATION", StepStatus.SUCCESS, "Created " + (comparisons != null ? comparisons : 0) + " comparison nodes");
        Integer queries = executeWithRetry(rawDocId, "QUERY_CREATION", () -> createQueryNodes(doc));
        saveStepLog(rawDocId, "QUERY_CREATION", StepStatus.SUCCESS, "Created " + (queries != null ? queries : 0) + " query nodes");

        // Step 5: Page Generation
        Page page = executeWithRetry(rawDocId, "PAGE_GENERATION", () -> generatePage(doc, scoreResult, entities, concepts));
        if (page == null) return;
        saveStepLog(rawDocId, "PAGE_GENERATION", StepStatus.SUCCESS, "Generated page: " + page.getSlug());

        // Step 6: Approval Submission
        executeWithRetry(rawDocId, "APPROVAL_SUBMISSION", () -> {
            submitForApproval(page.getId(), ApprovalAction.CREATE, "Auto-submitted after pipeline processing");
            return null;
        });
        saveStepLog(rawDocId, "APPROVAL_SUBMISSION", StepStatus.SUCCESS, "Submitted for approval: " + page.getId());

        // Step 7: Cross Linking
        Integer links = executeWithRetry(rawDocId, "CROSS_LINKING", () -> autoCrossLink(page, matchedNodes));
        saveStepLog(rawDocId, "CROSS_LINKING", StepStatus.SUCCESS, "Created " + (links != null ? links : 0) + " cross-links");

        // Step 8: Consistency Check
        ConsistencyReport consistencyReport = consistencyCheck(page, entities);
        saveStepLog(rawDocId, "CONSISTENCY_CHECK",
                consistencyReport.isPassed() ? StepStatus.SUCCESS : StepStatus.FAILED,
                consistencyReport.isPassed() ? "OK" : String.join("; ", consistencyReport.getIssues()));

        log.info("Pipeline completed for document: {}", doc.getId());
    }

    /**
     * Execute a pipeline step with retry. Returns null if all retries exhausted and saved to DLQ.
     */
    @SuppressWarnings("unchecked")
    private <T> T executeWithRetry(UUID rawDocId, String stepName, PipelineStep<T> step) {
        int maxRetries = getMaxRetries();
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                T result = step.execute();
                if (attempt > 1) {
                    log.info("Step {} succeeded on attempt {} for document {}", stepName, attempt, rawDocId);
                }
                return result;
            } catch (Exception e) {
                lastException = e;
                log.warn("Step {} failed (attempt {}/{}) for document {}: {}",
                        stepName, attempt, maxRetries, rawDocId, e.getMessage());

                if (attempt < maxRetries) {
                    // Exponential backoff: 1s, 2s, 4s...
                    long backoffMs = (long) Math.pow(2, attempt - 1) * 1000;
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // All retries exhausted — save to DLQ
        String errorMsg = lastException != null ? lastException.getMessage() : "Unknown error";
        log.error("Step {} failed after {} retries for document {}, saving to DLQ", stepName, maxRetries, rawDocId);
        saveToDeadLetterQueue(rawDocId, stepName, errorMsg, maxRetries);
        saveStepLog(rawDocId, stepName, StepStatus.FAILED, errorMsg);
        return null;
    }

    private void saveToDeadLetterQueue(UUID rawDocId, String step, String errorMessage, int retryCount) {
        DeadLetterQueue dlq = DeadLetterQueue.builder()
                .rawDocumentId(rawDocId)
                .step(step)
                .errorMessage(errorMessage)
                .retryCount(retryCount)
                .maxRetries(retryCount)
                .status("PENDING")
                .build();
        deadLetterQueueRepo.save(dlq);
    }

    private int getMaxRetries() {
        return configRepo.findByKey("pipeline.max.retries")
                .map(c -> {
                    try {
                        return Integer.parseInt(c.getValue());
                    } catch (NumberFormatException e) {
                        return DEFAULT_MAX_RETRIES;
                    }
                })
                .orElse(DEFAULT_MAX_RETRIES);
    }

    /**
     * Retry a dead letter entry by raw document ID.
     */
    @Transactional
    public void retryDeadLetter(UUID deadLetterId) {
        DeadLetterQueue dlq = deadLetterQueueRepo.findById(deadLetterId)
                .orElseThrow(() -> new IllegalArgumentException("Dead letter not found: " + deadLetterId));

        if (!"PENDING".equals(dlq.getStatus()) && !"FAILED".equals(dlq.getStatus())) {
            throw new IllegalStateException("Cannot retry dead letter with status: " + dlq.getStatus());
        }

        log.info("Retrying dead letter {} for document {} step {}", deadLetterId, dlq.getRawDocumentId(), dlq.getStep());

        // Reset status and increment retry count
        dlq.setStatus("RETRYING");
        dlq.setRetryCount(dlq.getRetryCount() + 1);
        deadLetterQueueRepo.save(dlq);

        try {
            processDocument(dlq.getRawDocumentId());
            dlq.setStatus("RESOLVED");
            deadLetterQueueRepo.save(dlq);
            log.info("Dead letter {} resolved successfully", deadLetterId);
        } catch (Exception e) {
            dlq.setStatus("FAILED");
            dlq.setErrorMessage(e.getMessage());
            deadLetterQueueRepo.save(dlq);
            log.error("Dead letter {} retry failed: {}", deadLetterId, e.getMessage());
            throw e;
        }
    }

    private void submitForApproval(UUID pageId, ApprovalAction action, String comment) {
        ApprovalQueue approval = ApprovalQueue.builder()
                .pageId(pageId)
                .action(action.name())
                .comment(comment)
                .status(ApprovalStatus.PENDING.name())
                .build();
        approvalQueueRepo.save(approval);
        log.info("Page {} auto-submitted for approval", pageId);
    }

    private ScoreResult scoreDocument(RawDocument doc) {
        return scoringService.scoreDocument(doc.getContent());
    }

    private ExtractionResult extractEntities(RawDocument doc) {
        List<ExampleData> examples = entityExampleService.loadExamplesAsExampleData(null);
        return aiClient.extractEntities(doc.getContent(), examples);
    }

    private ExtractionResult extractConcepts(RawDocument doc) {
        List<ExampleData> examples = entityExampleService.loadExamplesAsExampleData(null);
        return aiClient.extractConcepts(doc.getContent(), examples);
    }

    private List<KgNode> matchKnowledgeGraph(ExtractionResult entities, ExtractionResult concepts) {
        List<KgNode> matched = new ArrayList<>();
        // Build a name→node map for all entities in this document (for entity-entity edge creation)
        Map<String, KgNode> entityNodeMap = new LinkedHashMap<>();

        for (EntityInfo entity : entities.getEntities()) {
            Optional<KgNode> existing = kgNodeRepo.findByNameAndNodeType(entity.getName(), NodeType.ENTITY);
            if (existing.isPresent()) {
                KgNode node = existing.get();
                // P1-4: Update description if the new one is richer
                if (entity.getDescription() != null && !entity.getDescription().isEmpty()
                        && (node.getDescription() == null || node.getDescription().length() < entity.getDescription().length())) {
                    node.setDescription(buildDescriptionWithGrounding(entity.getDescription(), entity));
                    // Also update sub-type if previously null
                    if (node.getEntitySubType() == null && entity.getType() != null) {
                        node.setEntitySubType(entity.getType());
                    }
                    kgNodeRepo.save(node);
                }
                matched.add(node);
                entityNodeMap.put(entity.getName().toLowerCase(), node);
            } else {
                String description = buildDescriptionWithGrounding(entity.getDescription(), entity);
                KgNode node = kgNodeRepo.save(KgNode.builder()
                        .name(entity.getName())
                        .nodeType(NodeType.ENTITY)
                        .description(description)
                        .build());
                embedAndSave(node);
                matched.add(node);
                entityNodeMap.put(entity.getName().toLowerCase(), node);
                log.debug("Created KG entity node: {} (type={})", entity.getName(), entity.getType());
            }
        }

        // P0-2: Create entity-entity edges from entity relations
        for (EntityInfo entity : entities.getEntities()) {
            KgNode sourceNode = entityNodeMap.get(entity.getName().toLowerCase());
            if (sourceNode == null) continue;
            if (entity.getRelatedEntities() == null) continue;
            for (String relatedName : entity.getRelatedEntities()) {
                KgNode targetNode = entityNodeMap.get(relatedName.toLowerCase());
                if (targetNode != null) {
                    createEdgeIfNotExists(sourceNode.getId(), targetNode.getId(), EdgeType.RELATED_TO);
                }
            }
        }

        // E-6: Create edges from structured relations with type and confidence
        if (entities.getRelations() != null) {
            for (RelationInfo relation : entities.getRelations()) {
                if (!relation.hasValidType() || !relation.isConfident(0.5)) continue;
                KgNode sourceNode = entityNodeMap.get(relation.getSourceEntity().toLowerCase());
                KgNode targetNode = entityNodeMap.get(relation.getTargetEntity().toLowerCase());
                if (sourceNode != null && targetNode != null) {
                    EdgeType edgeType = mapRelationType(relation.getRelationType());
                    createEdgeIfNotExists(sourceNode.getId(), targetNode.getId(), edgeType, relation.getConfidence());
                }
            }
        }

        for (ConceptInfo concept : concepts.getConcepts()) {
            Optional<KgNode> existing = kgNodeRepo.findByNameAndNodeType(concept.getName(), NodeType.CONCEPT);
            final KgNode conceptNode;
            if (existing.isPresent()) {
                conceptNode = existing.get();
                // P1-4: Update description if the new one is richer
                if (concept.getDescription() != null && !concept.getDescription().isEmpty()
                        && (conceptNode.getDescription() == null || conceptNode.getDescription().length() < concept.getDescription().length())) {
                    conceptNode.setDescription(buildDescriptionWithGrounding(concept.getDescription(), concept));
                }
                matched.add(conceptNode);
            } else {
                String description = buildDescriptionWithGrounding(concept.getDescription(), concept);
                conceptNode = kgNodeRepo.save(KgNode.builder()
                        .name(concept.getName())
                        .nodeType(NodeType.CONCEPT)
                        .description(description)
                        .build());
                embedAndSave(conceptNode);
                matched.add(conceptNode);
                log.debug("Created KG concept node: {}", concept.getName());
            }

            // P1-5: Use case-insensitive matching for related entities
            for (String relatedName : concept.getRelatedEntities()) {
                kgNodeRepo.findByNameIgnoreCaseAndNodeType(relatedName, NodeType.ENTITY).ifPresent(relatedNode -> {
                    createEdgeIfNotExists(conceptNode.getId(), relatedNode.getId(), EdgeType.RELATED_TO);
                });
            }
        }

        return matched;
    }

    /**
     * Append source grounding metadata to the description when position info is available.
     */
    private String buildDescriptionWithGrounding(String baseDescription, ExtractionResult.EntityInfo entity) {
        if (entity.getStartOffset() == null) return baseDescription;
        StringBuilder sb = new StringBuilder();
        if (baseDescription != null) sb.append(baseDescription);
        sb.append(" [grounding: offset=").append(entity.getStartOffset())
          .append("-").append(entity.getEndOffset());
        if (entity.getAlignmentStatus() != null) {
            sb.append(", status=").append(entity.getAlignmentStatus());
        }
        if (entity.getExtractionIndex() != null) {
            sb.append(", index=").append(entity.getExtractionIndex());
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Append source grounding metadata to the description when position info is available.
     */
    private String buildDescriptionWithGrounding(String baseDescription, ExtractionResult.ConceptInfo concept) {
        if (concept.getStartOffset() == null) return baseDescription;
        StringBuilder sb = new StringBuilder();
        if (baseDescription != null) sb.append(baseDescription);
        sb.append(" [grounding: offset=").append(concept.getStartOffset())
          .append("-").append(concept.getEndOffset());
        if (concept.getAlignmentStatus() != null) {
            sb.append(", status=").append(concept.getAlignmentStatus());
        }
        if (concept.getExtractionIndex() != null) {
            sb.append(", index=").append(concept.getExtractionIndex());
        }
        sb.append("]");
        return sb.toString();
    }

    private void embedAndSave(KgNode node) {
        try {
            String text = node.getName() + ": " + (node.getDescription() != null ? node.getDescription() : "");
            float[] embedding = embeddingClient.embed(text);
            kgVectorRepo.save(KgVector.builder()
                    .nodeId(node.getId())
                    .vector(embedding)
                    .model("text-embedding-ada-002")
                    .build());
        } catch (Exception e) {
            log.warn("Failed to embed node {}: {}", node.getId(), e.getMessage());
        }
    }

    /**
     * 创建 COMPARISON 节点：当2+实体共享同一类型时，自动生成比较节点
     */
    private Integer createComparisonNodes(ExtractionResult entities) {
        int count = 0;
        java.util.Map<String, java.util.List<ExtractionResult.EntityInfo>> byType = new java.util.LinkedHashMap<>();
        for (var entity : entities.getEntities()) {
            if (entity.getType() != null) {
                byType.computeIfAbsent(entity.getType(), k -> new java.util.ArrayList<>()).add(entity);
            }
        }
        for (var entry : byType.entrySet()) {
            if (entry.getValue().size() >= 2) {
                String names = entry.getValue().stream()
                        .map(ExtractionResult.EntityInfo::getName)
                        .collect(java.util.stream.Collectors.joining(" vs "));
                String compName = "Comparison: " + names;
                Optional<KgNode> existing = kgNodeRepo.findByNameAndNodeType(compName, NodeType.COMPARISON);
                if (existing.isEmpty()) {
                    KgNode compNode = kgNodeRepo.save(KgNode.builder()
                            .name(compName)
                            .nodeType(NodeType.COMPARISON)
                            .description("Auto-generated comparison of " + entry.getKey() + " entities")
                            .build());
                    embedAndSave(compNode);
                    // Link comparison node to all entities in the group
                    for (var entity : entry.getValue()) {
                        kgNodeRepo.findByNameAndNodeType(entity.getName(), NodeType.ENTITY).ifPresent(entNode -> {
                            createEdgeIfNotExists(compNode.getId(), entNode.getId(), EdgeType.RELATED_TO);
                        });
                    }
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 创建 QUERY 节点：从内容中提取问题句（包含? 或以 What/How/Why 开头）
     */
    private Integer createQueryNodes(RawDocument doc) {
        int count = 0;
        if (doc.getContent() == null) return count;
        String[] lines = doc.getContent().split("\n");
        java.util.regex.Pattern questionPattern = java.util.regex.Pattern.compile("^(What|How|Why|When|Where|Who|Which|Is|Are|Can|Does)\\b", java.util.regex.Pattern.CASE_INSENSITIVE);
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.contains("?") || questionPattern.matcher(trimmed).find()) {
                String queryName = "Query: " + (trimmed.length() > 80 ? trimmed.substring(0, 80) + "..." : trimmed);
                Optional<KgNode> existing = kgNodeRepo.findByNameAndNodeType(queryName, NodeType.QUERY);
                if (existing.isEmpty()) {
                    KgNode queryNode = kgNodeRepo.save(KgNode.builder()
                            .name(queryName)
                            .nodeType(NodeType.QUERY)
                            .description("Auto-extracted question from document")
                            .build());
                    embedAndSave(queryNode);
                    count++;
                }
            }
        }
        return count;
    }

    private void createEdgeIfNotExists(UUID sourceId, UUID targetId, EdgeType type) {
        createEdgeIfNotExists(sourceId, targetId, type, 0.5);
    }

    private void createEdgeIfNotExists(UUID sourceId, UUID targetId, EdgeType type, double confidence) {
        // Check if edge already exists to avoid duplicates
        List<KgEdge> existing = kgEdgeRepo.findBySourceNodeId(sourceId);
        boolean alreadyExists = existing.stream()
                .anyMatch(e -> e.getTargetNodeId().equals(targetId) && e.getEdgeType() == type);
        if (alreadyExists) {
            log.debug("Edge {} -> {} ({}) already exists, skipping", sourceId, targetId, type);
            return;
        }
        kgEdgeRepo.save(KgEdge.builder()
                .sourceNodeId(sourceId)
                .targetNodeId(targetId)
                .edgeType(type)
                .weight(BigDecimal.valueOf(confidence))
                .build());
    }

    // E-6: Map relation type string to EdgeType enum
    private EdgeType mapRelationType(String relationType) {
        if (relationType == null || relationType.isBlank()) return EdgeType.RELATED_TO;
        try {
            return EdgeType.valueOf(relationType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown relation type: {}, falling back to RELATED_TO", relationType);
            return EdgeType.RELATED_TO;
        }
    }

    private Page generatePage(RawDocument doc, ScoreResult score, ExtractionResult entities, ExtractionResult concepts) {
        String slug = generateUniqueSlug(doc.getTitle());

        StringBuilder content = new StringBuilder();
        content.append("# ").append(doc.getTitle()).append("\n\n");
        content.append("> Score: ").append(score.getOverallScore()).append("/10\n");
        content.append("> Source: ").append(doc.getSourceUrl() != null ? doc.getSourceUrl() : "N/A").append("\n\n");

        if (!entities.getEntities().isEmpty()) {
            content.append("## Key Entities\n\n");
            for (EntityInfo e : entities.getEntities()) {
                content.append("- **").append(e.getName()).append("** (").append(e.getType()).append(")");
                if (e.getDescription() != null && !e.getDescription().isEmpty()) {
                    content.append(": ").append(e.getDescription());
                }
                content.append("\n");
            }
            content.append("\n");
        }

        if (!concepts.getConcepts().isEmpty()) {
            content.append("## Key Concepts\n\n");
            for (ConceptInfo c : concepts.getConcepts()) {
                content.append("- **").append(c.getName()).append("**");
                if (c.getDescription() != null && !c.getDescription().isEmpty()) {
                    content.append(": ").append(c.getDescription());
                }
                content.append("\n");
            }
            content.append("\n");
        }

        content.append("## Content\n\n");
        content.append(doc.getContent()).append("\n");

        Page page = pageRepo.save(Page.builder()
                .title(doc.getTitle())
                .slug(slug)
                .content(content.toString())
                .pageType(PageType.ENTITY)
                .status(PageStatus.PENDING_APPROVAL)
                .aiScore(score.getOverallScore())
                .sourceDocId(doc.getId())
                .build());

        for (String tag : score.getSuggestedTags()) {
            pageTagRepo.save(PageTag.builder().pageId(page.getId()).tag(tag).build());
        }

        // Generate document-level vector embedding
        embedPageContent(page);

        return page;
    }

    private void embedPageContent(Page page) {
        try {
            float[] embedding = embeddingClient.embed(page.getContent());
            // Store as JSON array string for H2 compatibility
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < embedding.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(embedding[i]);
            }
            sb.append("]");
            page.setContentVector(sb.toString());
            pageRepo.save(page);
        } catch (Exception e) {
            log.warn("Failed to embed page content {}: {}", page.getId(), e.getMessage());
        }
    }

    private String generateUniqueSlug(String title) {
        if (title == null) return "page-" + UUID.randomUUID().toString().substring(0, 8);
        String base = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .trim();
        if (base.length() > 100) base = base.substring(0, 100);
        if (base.isEmpty()) base = "page";

        // Ensure uniqueness
        String slug = base;
        int suffix = 1;
        while (pageRepo.findBySlug(slug).isPresent()) {
            slug = base + "-" + suffix;
            suffix++;
        }
        return slug;
    }

    private int autoCrossLink(Page page, List<KgNode> relatedNodes) {
        int count = 0;
        for (KgNode kgNode : relatedNodes) {
            if (kgNode.getPageId() != null && !kgNode.getPageId().equals(page.getId())) {
                // Avoid duplicate links
                List<PageLink> existingLinks = pageLinkRepo.findBySourcePageId(page.getId());
                boolean alreadyLinked = existingLinks.stream()
                        .anyMatch(l -> l.getTargetPageId().equals(kgNode.getPageId()));
                if (!alreadyLinked) {
                    pageLinkRepo.save(PageLink.builder()
                            .sourcePageId(page.getId())
                            .targetPageId(kgNode.getPageId())
                            .linkType(EdgeType.RELATED_TO)
                            .build());
                    count++;
                }
            }
        }
        return count;
    }

    private ConsistencyReport consistencyCheck(Page page, ExtractionResult entities) {
        List<String> issues = new ArrayList<>();
        int linkedPagesCount = 0;

        // 1. Check title
        if (page.getTitle() == null || page.getTitle().isEmpty()) {
            issues.add("Page title is empty");
        }

        // 2. Check content length
        if (page.getContent() == null || page.getContent().length() < 50) {
            issues.add("Page content is too short (minimum 50 chars)");
        }

        // 3. Check slug is URL-safe
        if (page.getSlug() == null || page.getSlug().isEmpty()) {
            issues.add("Page slug is empty");
        } else if (!page.getSlug().matches("[a-z0-9-]+")) {
            issues.add("Page slug contains invalid characters: " + page.getSlug());
        }

        // 4. Check all entity names appear in page content
        int entityCount = entities.getEntities().size();
        if (page.getContent() != null && entities.getEntities() != null) {
            for (var entity : entities.getEntities()) {
                if (entity.getName() != null && !page.getContent().contains(entity.getName())) {
                    issues.add("Entity '" + entity.getName() + "' not found in page content");
                }
            }
        }

        // 5. Check linked page IDs exist (no dangling links)
        List<PageLink> links = pageLinkRepo.findBySourcePageId(page.getId());
        for (PageLink link : links) {
            if (!pageRepo.existsById(link.getTargetPageId())) {
                issues.add("Dangling link to non-existent page: " + link.getTargetPageId());
            } else {
                linkedPagesCount++;
            }
        }

        boolean passed = issues.isEmpty();
        return ConsistencyReport.builder()
                .passed(passed)
                .issues(issues)
                .entityCount(entityCount)
                .linkedPagesCount(linkedPagesCount)
                .build();
    }

    private void saveStepLog(UUID rawDocId, String step, StepStatus status, String detail) {
        procLogRepo.save(ProcessingLog.builder()
                .rawDocumentId(rawDocId)
                .step(step)
                .status(status)
                .detail(detail)
                .build());
    }

    /**
     * Functional interface for pipeline steps that can throw exceptions.
     */
    @FunctionalInterface
    private interface PipelineStep<T> {
        T execute() throws Exception;
    }
}
