package com.llmwiki.service.search;

import com.llmwiki.adapter.api.AiApiClient;
import com.llmwiki.adapter.api.EmbeddingClient;
import com.llmwiki.common.dto.SearchRequest;
import com.llmwiki.domain.graph.entity.KgNode;
import com.llmwiki.domain.graph.repository.KgEdgeRepository;
import com.llmwiki.domain.graph.repository.KgNodeRepository;
import com.llmwiki.domain.page.entity.Page;
import com.llmwiki.domain.page.entity.PageTag;
import com.llmwiki.domain.page.repository.PageRepository;
import com.llmwiki.domain.page.repository.PageTagRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final EntityManager entityManager;
    private final KgNodeRepository nodeRepo;
    private final KgEdgeRepository edgeRepo;
    private final PageRepository pageRepo;
    private final PageTagRepository pageTagRepo;
    private final AiApiClient aiClient;
    private final EmbeddingClient embeddingClient;

    /**
     * Semantic search with optional type and tag filtering.
     */
    public List<SearchResult> search(SearchRequest request) {
        try {
            // Generate query embedding
            float[] queryEmbedding = embeddingClient.embed(request.getQuery());
            String vectorStr = embeddingToJson(queryEmbedding);
            int limit = request.getLimit() > 0 ? request.getLimit() : 10;

            // Use native query for vector similarity search via pgvector
            Query nativeQuery = entityManager.createNativeQuery(
                "SELECT v.node_id as nodeId, (v.vector <-> CAST(:queryVector AS vector)) as distance " +
                "FROM kg_vectors v ORDER BY v.vector <-> CAST(:queryVector AS vector) LIMIT :limit");
            nativeQuery.setParameter("queryVector", vectorStr);
            nativeQuery.setParameter("limit", limit * 3); // fetch extra for post-filtering
            @SuppressWarnings("unchecked")
            List<Object[]> results = nativeQuery.getResultList();

            List<SearchResult> searchResults = new ArrayList<>();
            for (Object[] row : results) {
                UUID nodeId = (UUID) row[0];
                double distance = ((Number) row[1]).doubleValue();
                double similarity = 1.0 / (1.0 + distance);

                Optional<KgNode> nodeOpt = nodeRepo.findById(nodeId);
                if (nodeOpt.isEmpty()) continue;
                KgNode node = nodeOpt.get();

                SearchResult result = new SearchResult();
                result.nodeId = nodeId;
                result.nodeName = node.getName();
                result.nodeType = node.getNodeType().name();
                result.description = node.getDescription();
                result.similarity = similarity;

                // Find associated pages
                if (node.getPageId() != null) {
                    pageRepo.findById(node.getPageId()).ifPresent(page -> {
                        result.pageTitle = page.getTitle();
                        result.pageSlug = page.getSlug();
                        result.pageContent = page.getContent();
                    });
                }

                searchResults.add(result);
            }

            // Post-filter by types if specified
            if (request.getTypes() != null && !request.getTypes().isEmpty()) {
                Set<String> typeSet = request.getTypes().stream()
                        .map(String::toUpperCase)
                        .collect(Collectors.toSet());
                searchResults = searchResults.stream()
                        .filter(r -> typeSet.contains(r.nodeType))
                        .collect(Collectors.toList());
            }

            // Post-filter by tags if specified
            if (request.getTags() != null && !request.getTags().isEmpty()) {
                Set<UUID> taggedNodeIds = new HashSet<>();
                for (String tag : request.getTags()) {
                    List<PageTag> pageTags = pageTagRepo.findByTagIgnoreCase(tag);
                    for (PageTag pt : pageTags) {
                        // Find nodes associated with tagged pages
                        pageRepo.findById(pt.getPageId()).ifPresent(page -> {
                            nodeRepo.findAll().stream()
                                    .filter(n -> pt.getPageId().equals(n.getPageId()))
                                    .forEach(n -> taggedNodeIds.add(n.getId()));
                        });
                    }
                }
                searchResults = searchResults.stream()
                        .filter(r -> taggedNodeIds.contains(r.nodeId))
                        .collect(Collectors.toList());
            }

            // Apply offset and limit
            int offset = request.getOffset();
            if (offset > 0 && offset < searchResults.size()) {
                searchResults = searchResults.subList(offset, searchResults.size());
            }
            if (searchResults.size() > limit) {
                searchResults = searchResults.subList(0, limit);
            }

            return searchResults;
        } catch (Exception e) {
            log.error("Search failed for query: {}", request.getQuery(), e);
            return fallbackKeywordSearch(request.getQuery(), request.getLimit());
        }
    }

    /**
     * Legacy search method for backward compatibility.
     */
    public List<SearchResult> search(String query, int limit) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .limit(limit)
                .build();
        return search(request);
    }

    /**
     * Find nodes by tag (exploratory search).
     */
    public List<SearchResult> searchByTag(String tag, int limit) {
        log.info("Searching by tag: {}", tag);
        List<SearchResult> results = new ArrayList<>();

        // Find all pages with the given tag
        List<PageTag> pageTags = pageTagRepo.findByTagIgnoreCase(tag);

        for (PageTag pageTag : pageTags) {
            pageRepo.findById(pageTag.getPageId()).ifPresent(page -> {
                // Find nodes associated with this page
                List<KgNode> nodes = nodeRepo.findAll().stream()
                        .filter(n -> pageTag.getPageId().equals(n.getPageId()))
                        .collect(Collectors.toList());
                for (KgNode node : nodes) {
                    SearchResult result = new SearchResult();
                    result.nodeId = node.getId();
                    result.nodeName = node.getName();
                    result.nodeType = node.getNodeType().name();
                    result.description = node.getDescription();
                    result.similarity = 1.0;
                    result.pageTitle = page.getTitle();
                    result.pageSlug = page.getSlug();
                    result.pageContent = page.getContent();
                    results.add(result);
                }
            });
        }

        // If no page-tag associations, try matching tag as node name/description
        if (results.isEmpty()) {
            List<KgNode> nodes = nodeRepo.findByNameContaining(tag);
            for (KgNode node : nodes) {
                SearchResult result = new SearchResult();
                result.nodeId = node.getId();
                result.nodeName = node.getName();
                result.nodeType = node.getNodeType().name();
                result.description = node.getDescription();
                result.similarity = 0.8;
                if (node.getPageId() != null) {
                    pageRepo.findById(node.getPageId()).ifPresent(page -> {
                        result.pageTitle = page.getTitle();
                        result.pageSlug = page.getSlug();
                    });
                }
                results.add(result);
            }
        }

        return results.stream().distinct().limit(limit).collect(Collectors.toList());
    }

    /**
     * Find nodes by relation (exploratory search).
     */
    public List<SearchResult> searchByRelation(UUID nodeId, String relationType, int limit) {
        log.info("Searching by relation: nodeId={}, relationType={}", nodeId, relationType);
        List<SearchResult> results = new ArrayList<>();

        // Find the source node
        Optional<KgNode> sourceOpt = nodeRepo.findById(nodeId);
        if (sourceOpt.isEmpty()) {
            return results;
        }
        KgNode sourceNode = sourceOpt.get();

        // Find edges matching the relation type
        List<UUID> relatedNodeIds = new ArrayList<>();

        if (relationType != null && !relationType.isBlank()) {
            // Filter by specific relation type
            relatedNodeIds.addAll(
                edgeRepo.findBySourceNodeId(nodeId).stream()
                        .filter(e -> e.getEdgeType().name().equalsIgnoreCase(relationType))
                        .map(e -> e.getTargetNodeId())
                        .collect(Collectors.toList())
            );
            relatedNodeIds.addAll(
                edgeRepo.findByTargetNodeId(nodeId).stream()
                        .filter(e -> e.getEdgeType().name().equalsIgnoreCase(relationType))
                        .map(e -> e.getSourceNodeId())
                        .collect(Collectors.toList())
            );
        } else {
            // Any relation type
            relatedNodeIds.addAll(
                edgeRepo.findBySourceNodeId(nodeId).stream()
                        .map(e -> e.getTargetNodeId())
                        .collect(Collectors.toList())
            );
            relatedNodeIds.addAll(
                edgeRepo.findByTargetNodeId(nodeId).stream()
                        .map(e -> e.getSourceNodeId())
                        .collect(Collectors.toList())
            );
        }

        // Build results from related nodes
        for (UUID relatedId : relatedNodeIds) {
            nodeRepo.findById(relatedId).ifPresent(node -> {
                SearchResult result = new SearchResult();
                result.nodeId = node.getId();
                result.nodeName = node.getName();
                result.nodeType = node.getNodeType().name();
                result.description = node.getDescription();
                result.similarity = 0.9;
                if (node.getPageId() != null) {
                    pageRepo.findById(node.getPageId()).ifPresent(page -> {
                        result.pageTitle = page.getTitle();
                        result.pageSlug = page.getSlug();
                    });
                }
                results.add(result);
            });
        }

        // Also include the source node itself
        SearchResult sourceResult = new SearchResult();
        sourceResult.nodeId = sourceNode.getId();
        sourceResult.nodeName = sourceNode.getName();
        sourceResult.nodeType = sourceNode.getNodeType().name();
        sourceResult.description = sourceNode.getDescription();
        sourceResult.similarity = 1.0;
        if (sourceNode.getPageId() != null) {
            pageRepo.findById(sourceNode.getPageId()).ifPresent(page -> {
                sourceResult.pageTitle = page.getTitle();
                sourceResult.pageSlug = page.getSlug();
            });
        }
        results.add(0, sourceResult);

        return results.stream().distinct().limit(limit).collect(Collectors.toList());
    }

    /**
     * Natural language Q&A. Searches knowledge base first, then falls back to LLM.
     */
    public AnswerResult ask(String question) {
        // Step 1: Search relevant content
        List<SearchResult> results = search(question, 5);

        if (results.isEmpty()) {
            // Fallback: ask LLM directly
            String llmAnswer = aiClient.chat(
                    "You are a helpful assistant. Answer the question based on your knowledge. " +
                    "If you don't know, say so clearly.",
                    question);
            return new AnswerResult(llmAnswer, "LLM_FALLBACK", Collections.emptyList());
        }

        // Step 2: Build context from search results
        StringBuilder context = new StringBuilder();
        for (SearchResult r : results) {
            context.append("## ").append(r.nodeName).append("\n");
            if (r.description != null) context.append(r.description).append("\n");
            if (r.pageContent != null) {
                context.append(r.pageContent, 0, Math.min(r.pageContent.length(), 500)).append("\n");
            }
            context.append("\n");
        }

        // Step 3: Ask LLM with context
        String answer = aiClient.chat(
                "You are a knowledge base assistant. Answer the question using ONLY the provided context. " +
                "If the context doesn't contain the answer, say 'I don't have enough information in the knowledge base to answer this.' " +
                "Always cite your sources.\n\nContext:\n" + context,
                question);

        List<String> sources = results.stream()
                .map(r -> r.nodeName + " (" + r.nodeType + ")")
                .collect(Collectors.toList());

        return new AnswerResult(answer, "KNOWLEDGE_BASE", sources);
    }

    /**
     * Fallback keyword-based search when vector search fails.
     */
    private List<SearchResult> fallbackKeywordSearch(String query, int limit) {
        String[] keywords = query.toLowerCase().split("\\s+");
        List<SearchResult> results = new ArrayList<>();

        for (String keyword : keywords) {
            List<KgNode> nodes = nodeRepo.findByNameContaining(keyword);
            for (KgNode node : nodes) {
                SearchResult result = new SearchResult();
                result.nodeId = node.getId();
                result.nodeName = node.getName();
                result.nodeType = node.getNodeType().name();
                result.description = node.getDescription();
                result.similarity = 0.5; // Placeholder
                if (node.getPageId() != null) {
                    pageRepo.findById(node.getPageId()).ifPresent(page -> {
                        result.pageTitle = page.getTitle();
                        result.pageSlug = page.getSlug();
                    });
                }
                results.add(result);
            }
        }

        return results.stream().distinct().limit(limit).collect(Collectors.toList());
    }

    private String embeddingToJson(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    // =============================================
    // Result DTOs
    // =============================================
    public static class SearchResult {
        public UUID nodeId;
        public String nodeName;
        public String nodeType;
        public String description;
        public double similarity;
        public String pageTitle;
        public String pageSlug;
        public String pageContent;
    }

    public static class AnswerResult {
        public String answer;
        public String source; // KNOWLEDGE_BASE or LLM_FALLBACK
        public List<String> citations;

        public AnswerResult(String answer, String source, List<String> citations) {
            this.answer = answer;
            this.source = source;
            this.citations = citations;
        }
    }
}
