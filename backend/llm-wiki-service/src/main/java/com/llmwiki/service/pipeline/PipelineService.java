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
import com.llmwiki.domain.processing.entity.ProcessingLog;
import com.llmwiki.domain.processing.repository.ProcessingLogRepository;
import com.llmwiki.domain.sync.entity.RawDocument;
import com.llmwiki.domain.sync.repository.RawDocumentRepository;
import com.llmwiki.domain.config.repository.SystemConfigRepository;
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

    private final RawDocumentRepository rawDocRepo;
    private final ProcessingLogRepository procLogRepo;
    private final KgNodeRepository kgNodeRepo;
    private final KgEdgeRepository kgEdgeRepo;
    private final KgVectorRepository kgVectorRepo;
    private final PageRepository pageRepo;
    private final PageLinkRepository pageLinkRepo;
    private final PageTagRepository pageTagRepo;
    private final ApprovalQueueRepository approvalQueueRepo;
    private final SystemConfigRepository configRepo;

    private final AiApiClient aiClient;
    private final EmbeddingClient embeddingClient;

    @Transactional
    public void processDocument(UUID rawDocId) {
        RawDocument doc = rawDocRepo.findById(rawDocId)
                .orElseThrow(() -> new IllegalArgumentException("Raw document not found: " + rawDocId));

        log.info("Starting pipeline for document: {} ({})", doc.getTitle(), doc.getId());

        BigDecimal threshold = getScoreThreshold();
        ScoreResult scoreResult = scoreDocument(doc);
        if (scoreResult.getOverallScore().compareTo(threshold) < 0) {
            log.info("Document {} score {} below threshold {}, skipping", doc.getId(), scoreResult.getOverallScore(), threshold);
            saveStepLog(rawDocId, "SCORE", StepStatus.SKIPPED, "Score " + scoreResult.getOverallScore() + " below threshold " + threshold);
            return;
        }
        saveStepLog(rawDocId, "SCORE", StepStatus.SUCCESS, scoreResult.getReason());

        ExtractionResult entities = extractEntities(doc);
        saveStepLog(rawDocId, "ENTITY_EXTRACTION", StepStatus.SUCCESS, "Extracted " + entities.getEntities().size() + " entities");

        ExtractionResult concepts = extractConcepts(doc);
        saveStepLog(rawDocId, "CONCEPT_EXTRACTION", StepStatus.SUCCESS, "Extracted " + concepts.getConcepts().size() + " concepts");

        List<KgNode> matchedNodes = matchKnowledgeGraph(entities, concepts);
        saveStepLog(rawDocId, "GRAPH_MATCHING", StepStatus.SUCCESS, "Matched " + matchedNodes.size() + " graph nodes");

        Page page = generatePage(doc, scoreResult, entities, concepts);
        saveStepLog(rawDocId, "PAGE_GENERATION", StepStatus.SUCCESS, "Generated page: " + page.getSlug());

        // Auto-submit for approval
        submitForApproval(page.getId(), ApprovalAction.CREATE, "Auto-submitted after pipeline processing");
        saveStepLog(rawDocId, "APPROVAL_SUBMISSION", StepStatus.SUCCESS, "Submitted for approval: " + page.getId());

        int links = autoCrossLink(page, matchedNodes);
        saveStepLog(rawDocId, "CROSS_LINKING", StepStatus.SUCCESS, "Created " + links + " cross-links");

        boolean consistent = consistencyCheck(page);
        saveStepLog(rawDocId, "CONSISTENCY_CHECK", consistent ? StepStatus.SUCCESS : StepStatus.FAILED, consistent ? "OK" : "Issues found");

        log.info("Pipeline completed for document: {}", doc.getId());
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
        return aiClient.score(doc.getContent());
    }

    private BigDecimal getScoreThreshold() {
        return configRepo.findByKey("scoring.threshold")
                .map(c -> new BigDecimal(c.getValue()))
                .orElse(new BigDecimal("60.0"));
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
        content.append("> Score: ").append(score.getOverallScore()).append("/100\n");
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
}
