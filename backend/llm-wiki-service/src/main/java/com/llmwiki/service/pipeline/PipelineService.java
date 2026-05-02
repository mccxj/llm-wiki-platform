package com.llmwiki.service.pipeline;

import com.llmwiki.adapter.api.AiApiClient;
import com.llmwiki.adapter.api.EmbeddingClient;
import com.llmwiki.adapter.dto.ExtractionResult;
import com.llmwiki.adapter.dto.ExtractionResult.ConceptInfo;
import com.llmwiki.adapter.dto.ExtractionResult.EntityInfo;
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

    @Transactional
    public void processDocument(UUID rawDocId) {
        RawDocument doc = rawDocRepo.findById(rawDocId)
                .orElseThrow(() -> new IllegalArgumentException("Raw document not found: " + rawDocId));

        log.info("Starting pipeline for document: {} ({})", doc.getTitle(), doc.getId());

        // Step 1: Score
        ScoreResult scoreResult = executeWithRetry(rawDocId, "SCORE", () -> scoreDocument(doc));
        if (scoreResult == null) {
            return; // already in DLQ
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
        boolean consistent = consistencyCheck(page);
        saveStepLog(rawDocId, "CONSISTENCY_CHECK", consistent ? StepStatus.SUCCESS : StepStatus.FAILED, consistent ? "OK" : "Issues found");

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
        return aiClient.extractEntities(doc.getContent());
    }

    private ExtractionResult extractConcepts(RawDocument doc) {
        return aiClient.extractConcepts(doc.getContent());
    }

    private List<KgNode> matchKnowledgeGraph(ExtractionResult entities, ExtractionResult concepts) {
        List<KgNode> matched = new ArrayList<>();

        for (EntityInfo entity : entities.getEntities()) {
            Optional<KgNode> existing = kgNodeRepo.findByNameAndNodeType(entity.getName(), NodeType.ENTITY);
            if (existing.isPresent()) {
                matched.add(existing.get());
            } else {
                KgNode node = kgNodeRepo.save(KgNode.builder()
                        .name(entity.getName())
                        .nodeType(NodeType.ENTITY)
                        .description(entity.getDescription())
                        .build());
                embedAndSave(node);
                matched.add(node);
                log.debug("Created KG entity node: {}", entity.getName());
            }
        }

        for (ConceptInfo concept : concepts.getConcepts()) {
            Optional<KgNode> existing = kgNodeRepo.findByNameAndNodeType(concept.getName(), NodeType.CONCEPT);
            final KgNode conceptNode;
            if (existing.isPresent()) {
                conceptNode = existing.get();
                matched.add(conceptNode);
            } else {
                conceptNode = kgNodeRepo.save(KgNode.builder()
                        .name(concept.getName())
                        .nodeType(NodeType.CONCEPT)
                        .description(concept.getDescription())
                        .build());
                embedAndSave(conceptNode);
                matched.add(conceptNode);
                log.debug("Created KG concept node: {}", concept.getName());
            }

            for (String relatedName : concept.getRelatedEntities()) {
                kgNodeRepo.findByNameAndNodeType(relatedName, NodeType.ENTITY).ifPresent(relatedNode -> {
                    createEdgeIfNotExists(conceptNode.getId(), relatedNode.getId(), EdgeType.RELATED_TO);
                });
            }
        }

        return matched;
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

    private void createEdgeIfNotExists(UUID sourceId, UUID targetId, EdgeType type) {
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
                .weight(BigDecimal.valueOf(0.5))
                .build());
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

        return page;
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

    private boolean consistencyCheck(Page page) {
        return page.getTitle() != null && !page.getTitle().isEmpty()
                && page.getContent() != null && page.getContent().length() >= 50;
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
