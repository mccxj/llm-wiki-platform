package com.llmwiki.service.search;

import com.llmwiki.adapter.api.AiApiClient;
import com.llmwiki.adapter.api.EmbeddingClient;
import com.llmwiki.domain.graph.entity.KgNode;
import com.llmwiki.domain.graph.repository.KgNodeRepository;
import com.llmwiki.domain.page.entity.Page;
import com.llmwiki.domain.page.repository.PageRepository;
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
    private final PageRepository pageRepo;
    private final AiApiClient aiClient;
    private final EmbeddingClient embeddingClient;

    /**
     * Semantic search: find pages most similar to the query.
     */
    public List<SearchResult> search(String query, int limit) {
        try {
            // Generate query embedding
            float[] queryEmbedding = embeddingClient.embed(query);
            String vectorStr = embeddingToJson(queryEmbedding);

            // Use native query for vector similarity search via pgvector
            // The <-> operator computes Euclidean distance (smaller = more similar)
            Query nativeQuery = entityManager.createNativeQuery(
                "SELECT v.node_id as nodeId, (v.vector <-> CAST(:queryVector AS vector)) as distance " +
                "FROM kg_vectors v ORDER BY v.vector <-> CAST(:queryVector AS vector) LIMIT :limit");
            nativeQuery.setParameter("queryVector", vectorStr);
            nativeQuery.setParameter("limit", limit);
            @SuppressWarnings("unchecked")
            List<Object[]> results = nativeQuery.getResultList();

            List<SearchResult> searchResults = new ArrayList<>();
            for (Object[] row : results) {
                UUID nodeId = (UUID) row[0];
                double distance = ((Number) row[1]).doubleValue();
                double similarity = 1.0 / (1.0 + distance); // Convert distance to similarity

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

            return searchResults;
        } catch (Exception e) {
            log.error("Search failed for query: {}", query, e);
            return fallbackKeywordSearch(query, limit);
        }
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
