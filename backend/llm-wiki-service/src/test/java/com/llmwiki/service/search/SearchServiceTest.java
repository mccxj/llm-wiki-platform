package com.llmwiki.service.search;

import com.llmwiki.adapter.api.AiApiClient;
import com.llmwiki.adapter.api.EmbeddingClient;
import com.llmwiki.common.dto.SearchRequest;
import com.llmwiki.common.enums.EdgeType;
import com.llmwiki.common.enums.NodeType;
import com.llmwiki.common.enums.PageStatus;
import com.llmwiki.domain.graph.entity.KgEdge;
import com.llmwiki.domain.graph.entity.KgNode;
import com.llmwiki.domain.graph.repository.KgEdgeRepository;
import com.llmwiki.domain.graph.repository.KgNodeRepository;
import com.llmwiki.domain.page.entity.Page;
import com.llmwiki.domain.page.entity.PageTag;
import com.llmwiki.domain.page.repository.PageRepository;
import com.llmwiki.domain.page.repository.PageTagRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock EntityManager entityManager;
    @Mock KgNodeRepository nodeRepo;
    @Mock KgEdgeRepository edgeRepo;
    @Mock PageRepository pageRepo;
    @Mock PageTagRepository pageTagRepo;
    @Mock AiApiClient aiClient;
    @Mock EmbeddingClient embeddingClient;
    @Mock Query nativeQuery;

    @InjectMocks
    SearchService searchService;

    KgNode node;
    Page page;
    UUID nodeId;
    UUID pageId;

    @BeforeEach
    void setUp() {
        nodeId = UUID.randomUUID();
        pageId = UUID.randomUUID();
        node = KgNode.builder()
                .id(nodeId).name("Java").nodeType(NodeType.ENTITY)
                .description("A programming language").pageId(pageId).build();
        page = Page.builder()
                .id(pageId).title("Java Programming").slug("java-programming")
                .content("Java is a programming language...")
                .status(PageStatus.APPROVED).aiScore(new BigDecimal("8.5")).build();
    }

    @SuppressWarnings("unchecked")
    private void setupVectorSearch(List<Object[]> rows) {
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        when(nativeQuery.getResultList()).thenReturn(rows);
    }

    @Test
    void search_shouldReturnResultsWithSimilarity() {
        float[] queryEmbedding = new float[1536];
        queryEmbedding[0] = 1.0f;

        Object[] row = {nodeId, 0.5};
        List<Object[]> rows = Collections.singletonList(row);

        when(embeddingClient.embed("java")).thenReturn(queryEmbedding);
        setupVectorSearch(rows);
        when(nodeRepo.findById(nodeId)).thenReturn(Optional.of(node));
        when(pageRepo.findById(pageId)).thenReturn(Optional.of(page));

        SearchRequest request = SearchRequest.builder().query("java").limit(5).build();
        List<SearchService.SearchResult> results = searchService.search(request);

        assertEquals(1, results.size());
        SearchService.SearchResult result = results.get(0);
        assertEquals(nodeId, result.nodeId);
        assertEquals("Java", result.nodeName);
        assertEquals("ENTITY", result.nodeType);
        assertEquals("A programming language", result.description);
        assertEquals(1.0 / (1.0 + 0.5), result.similarity, 0.001);
        assertEquals("Java Programming", result.pageTitle);
        assertEquals("java-programming", result.pageSlug);
    }

    @Test
    void search_shouldHandleEmptyResults() {
        float[] queryEmbedding = new float[1536];
        when(embeddingClient.embed("unknown")).thenReturn(queryEmbedding);
        setupVectorSearch(Collections.emptyList());

        SearchRequest request = SearchRequest.builder().query("unknown").limit(5).build();
        List<SearchService.SearchResult> results = searchService.search(request);

        assertTrue(results.isEmpty());
    }

    @Test
    void search_shouldFallbackToKeywordSearchOnException() {
        when(embeddingClient.embed("java")).thenThrow(new RuntimeException("Embedding failed"));
        when(nodeRepo.findByNameContaining("java")).thenReturn(List.of(node));
        when(pageRepo.findById(pageId)).thenReturn(Optional.of(page));

        SearchRequest request = SearchRequest.builder().query("java").limit(5).build();
        List<SearchService.SearchResult> results = searchService.search(request);

        assertEquals(1, results.size());
        assertEquals("Java", results.get(0).nodeName);
        assertEquals(0.5, results.get(0).similarity);
    }

    @Test
    void search_legacyMethod_shouldWork() {
        float[] queryEmbedding = new float[1536];
        Object[] row = {nodeId, 0.5};
        List<Object[]> rows = Collections.singletonList(row);

        when(embeddingClient.embed("java")).thenReturn(queryEmbedding);
        setupVectorSearch(rows);
        when(nodeRepo.findById(nodeId)).thenReturn(Optional.of(node));
        when(pageRepo.findById(pageId)).thenReturn(Optional.of(page));

        List<SearchService.SearchResult> results = searchService.search("java", 5);

        assertEquals(1, results.size());
        assertEquals("Java", results.get(0).nodeName);
    }

    @Test
    void search_shouldFilterByType() {
        float[] queryEmbedding = new float[1536];
        queryEmbedding[0] = 1.0f;

        UUID nodeId2 = UUID.randomUUID();
        KgNode conceptNode = KgNode.builder()
                .id(nodeId2).name("OOP").nodeType(NodeType.CONCEPT)
                .description("Object-oriented programming").build();

        Object[] row1 = {nodeId, 0.3};
        Object[] row2 = {nodeId2, 0.5};

        when(embeddingClient.embed("test")).thenReturn(queryEmbedding);
        setupVectorSearch(List.of(row1, row2));
        when(nodeRepo.findById(nodeId)).thenReturn(Optional.of(node));
        when(nodeRepo.findById(nodeId2)).thenReturn(Optional.of(conceptNode));
        when(pageRepo.findById(pageId)).thenReturn(Optional.of(page));

        SearchRequest request = SearchRequest.builder()
                .query("test")
                .types(List.of("ENTITY"))
                .limit(10)
                .build();
        List<SearchService.SearchResult> results = searchService.search(request);

        assertEquals(1, results.size());
        assertEquals("ENTITY", results.get(0).nodeType);
    }

    @Test
    void search_shouldFilterByTag() {
        float[] queryEmbedding = new float[1536];
        queryEmbedding[0] = 1.0f;

        UUID tagPageId = UUID.randomUUID();
        KgNode taggedNode = KgNode.builder()
                .id(nodeId).name("Java").nodeType(NodeType.ENTITY)
                .description("A programming language").pageId(tagPageId).build();
        KgNode otherNode = KgNode.builder()
                .id(UUID.randomUUID()).name("Python").nodeType(NodeType.ENTITY)
                .description("Another language").build();

        Object[] row1 = {nodeId, 0.3};
        Object[] row2 = {otherNode.getId(), 0.5};

        PageTag pageTag = PageTag.builder().pageId(tagPageId).tag("jvm").build();

        when(embeddingClient.embed("test")).thenReturn(queryEmbedding);
        setupVectorSearch(List.of(row1, row2));
        when(nodeRepo.findById(nodeId)).thenReturn(Optional.of(taggedNode));
        when(nodeRepo.findById(otherNode.getId())).thenReturn(Optional.of(otherNode));
        when(pageRepo.findById(tagPageId)).thenReturn(Optional.of(page));
        when(pageTagRepo.findByTagIgnoreCase("jvm")).thenReturn(List.of(pageTag));
        when(nodeRepo.findAll()).thenReturn(List.of(taggedNode));

        SearchRequest request = SearchRequest.builder()
                .query("test")
                .tags(List.of("jvm"))
                .limit(10)
                .build();
        List<SearchService.SearchResult> results = searchService.search(request);

        assertEquals(1, results.size());
        assertEquals("Java", results.get(0).nodeName);
        verify(pageTagRepo).findByTagIgnoreCase("jvm");
        verify(pageTagRepo, never()).findAll();
    }

    @Test
    void searchByTag_shouldReturnResultsFromPageTags() {
        UUID tagPageId = UUID.randomUUID();
        PageTag pageTag = PageTag.builder().pageId(tagPageId).tag("java").build();
        KgNode taggedNode = KgNode.builder()
                .id(UUID.randomUUID()).name("Spring").nodeType(NodeType.ENTITY)
                .description("Spring framework").pageId(tagPageId).build();
        Page taggedPage = Page.builder()
                .id(tagPageId).title("Spring Framework").slug("spring")
                .content("Spring is a framework...").status(PageStatus.APPROVED).build();

        when(pageTagRepo.findByTagIgnoreCase("java")).thenReturn(List.of(pageTag));
        when(pageRepo.findById(tagPageId)).thenReturn(Optional.of(taggedPage));
        when(nodeRepo.findAll()).thenReturn(List.of(taggedNode));

        List<SearchService.SearchResult> results = searchService.searchByTag("java", 20);

        assertFalse(results.isEmpty());
        assertEquals("Spring", results.get(0).nodeName);
        verify(pageTagRepo).findByTagIgnoreCase("java");
        verify(pageTagRepo, never()).findAll();
    }

    @Test
    void searchByTag_shouldFallbackToNameMatch() {
        when(pageTagRepo.findByTagIgnoreCase("python")).thenReturn(Collections.emptyList());
        when(nodeRepo.findByNameContaining("python")).thenReturn(List.of(node));

        List<SearchService.SearchResult> results = searchService.searchByTag("python", 20);

        assertFalse(results.isEmpty());
        assertEquals("Java", results.get(0).nodeName);
        verify(pageTagRepo).findByTagIgnoreCase("python");
        verify(pageTagRepo, never()).findAll();
    }

    @Test
    void searchByRelation_shouldReturnRelatedNodes() {
        UUID relatedNodeId = UUID.randomUUID();
        KgNode relatedNode = KgNode.builder()
                .id(relatedNodeId).name("Kotlin").nodeType(NodeType.ENTITY)
                .description("Kotlin language").build();
        KgEdge edge = KgEdge.builder()
                .sourceNodeId(nodeId).targetNodeId(relatedNodeId)
                .edgeType(EdgeType.RELATED_TO).build();

        when(nodeRepo.findById(nodeId)).thenReturn(Optional.of(node));
        when(edgeRepo.findBySourceNodeId(nodeId)).thenReturn(List.of(edge));
        when(edgeRepo.findByTargetNodeId(nodeId)).thenReturn(Collections.emptyList());
        when(nodeRepo.findById(relatedNodeId)).thenReturn(Optional.of(relatedNode));

        List<SearchService.SearchResult> results = searchService.searchByRelation(nodeId, "RELATED_TO", 20);

        assertTrue(results.size() >= 2); // source node + related node
        assertTrue(results.stream().anyMatch(r -> r.nodeName.equals("Java")));
        assertTrue(results.stream().anyMatch(r -> r.nodeName.equals("Kotlin")));
    }

    @Test
    void searchByRelation_shouldIncludeSourceNode() {
        when(nodeRepo.findById(nodeId)).thenReturn(Optional.of(node));
        when(edgeRepo.findBySourceNodeId(nodeId)).thenReturn(Collections.emptyList());
        when(edgeRepo.findByTargetNodeId(nodeId)).thenReturn(Collections.emptyList());

        List<SearchService.SearchResult> results = searchService.searchByRelation(nodeId, null, 20);

        assertEquals(1, results.size());
        assertEquals("Java", results.get(0).nodeName);
    }

    @Test
    void searchByRelation_shouldHandleEmptySource() {
        when(nodeRepo.findById(nodeId)).thenReturn(Optional.empty());

        List<SearchService.SearchResult> results = searchService.searchByRelation(nodeId, "RELATED_TO", 20);

        assertTrue(results.isEmpty());
    }

    @Test
    void ask_shouldReturnAnswerFromKnowledgeBase() {
        float[] queryEmbedding = new float[1536];
        Object[] row = {nodeId, 0.3};
        List<Object[]> rows = Collections.singletonList(row);

        when(embeddingClient.embed("What is Java?")).thenReturn(queryEmbedding);
        setupVectorSearch(rows);
        when(nodeRepo.findById(nodeId)).thenReturn(Optional.of(node));
        when(pageRepo.findById(pageId)).thenReturn(Optional.of(page));
        when(aiClient.chat(contains("knowledge base"), eq("What is Java?")))
                .thenReturn("Java is a programming language.");

        SearchService.AnswerResult result = searchService.ask("What is Java?");

        assertNotNull(result);
        assertEquals("Java is a programming language.", result.answer);
        assertEquals("KNOWLEDGE_BASE", result.source);
        assertFalse(result.citations.isEmpty());
        assertTrue(result.citations.get(0).contains("Java"));
    }

    @Test
    void ask_shouldFallbackToLLMWhenNoResults() {
        float[] queryEmbedding = new float[1536];
        when(embeddingClient.embed("obscure question")).thenReturn(queryEmbedding);
        setupVectorSearch(Collections.emptyList());
        when(aiClient.chat(contains("helpful assistant"), eq("obscure question")))
                .thenReturn("I don't have specific information.");

        SearchService.AnswerResult result = searchService.ask("obscure question");

        assertNotNull(result);
        assertEquals("I don't have specific information.", result.answer);
        assertEquals("LLM_FALLBACK", result.source);
        assertTrue(result.citations.isEmpty());
    }

    @Test
    void ask_shouldTruncateLongContent() {
        float[] queryEmbedding = new float[1536];
        Object[] row = {nodeId, 0.3};
        List<Object[]> rows = Collections.singletonList(row);

        String longContent = "A".repeat(1000);
        Page longPage = Page.builder()
                .id(pageId).title("Long").slug("long")
                .content(longContent).status(PageStatus.APPROVED).build();

        when(embeddingClient.embed("test")).thenReturn(queryEmbedding);
        setupVectorSearch(rows);
        when(nodeRepo.findById(nodeId)).thenReturn(Optional.of(node));
        when(pageRepo.findById(pageId)).thenReturn(Optional.of(longPage));
        when(aiClient.chat(anyString(), eq("test"))).thenReturn("Answer");

        SearchService.AnswerResult result = searchService.ask("test");

        assertNotNull(result);
        verify(aiClient).chat(anyString(), eq("test"));
    }

    @Test
    void search_shouldApplyOffsetAndLimit() {
        float[] queryEmbedding = new float[1536];
        UUID nodeId2 = UUID.randomUUID();
        KgNode node2 = KgNode.builder()
                .id(nodeId2).name("Python").nodeType(NodeType.ENTITY)
                .description("Python language").build();

        Object[] row1 = {nodeId, 0.3};
        Object[] row2 = {nodeId2, 0.5};

        when(embeddingClient.embed("test")).thenReturn(queryEmbedding);
        setupVectorSearch(java.util.Arrays.asList(row1, row2));
        when(nodeRepo.findById(nodeId)).thenReturn(Optional.of(node));
        when(nodeRepo.findById(nodeId2)).thenReturn(Optional.of(node2));
        when(pageRepo.findById(pageId)).thenReturn(Optional.of(page));

        SearchRequest request = SearchRequest.builder()
                .query("test").limit(1).offset(1).build();
        List<SearchService.SearchResult> results = searchService.search(request);

        assertEquals(1, results.size());
        assertEquals("Python", results.get(0).nodeName);
    }

    @Test
    void search_shouldReturnAllWhenOffsetExceedsResults() {
        float[] queryEmbedding = new float[1536];
        Object[] row = {nodeId, 0.3};

        when(embeddingClient.embed("test")).thenReturn(queryEmbedding);
        setupVectorSearch(java.util.Collections.singletonList(row));
        when(nodeRepo.findById(nodeId)).thenReturn(Optional.of(node));

        SearchRequest request = SearchRequest.builder()
                .query("test").limit(10).offset(5).build();
        List<SearchService.SearchResult> results = searchService.search(request);

        assertEquals(1, results.size());
    }

    @Test
    void searchByTag_shouldReturnEmptyForNoMatch() {
        when(pageTagRepo.findByTagIgnoreCase("nonexistent")).thenReturn(List.of());
        when(nodeRepo.findByNameContaining("nonexistent")).thenReturn(List.of());

        List<SearchService.SearchResult> results = searchService.searchByTag("nonexistent", 20);

        assertTrue(results.isEmpty());
    }

    @Test
    void searchByTag_shouldDeduplicateResults() {
        UUID tagPageId = UUID.randomUUID();
        PageTag pageTag = PageTag.builder().pageId(tagPageId).tag("java").build();
        KgNode taggedNode = KgNode.builder()
                .id(UUID.randomUUID()).name("Spring").nodeType(NodeType.ENTITY)
                .description("Spring framework").pageId(tagPageId).build();
        Page taggedPage = Page.builder()
                .id(tagPageId).title("Spring Framework").slug("spring")
                .content("Spring is a framework...").status(PageStatus.APPROVED).build();

        when(pageTagRepo.findByTagIgnoreCase("java")).thenReturn(List.of(pageTag));
        when(pageRepo.findById(tagPageId)).thenReturn(Optional.of(taggedPage));
        when(nodeRepo.findAll()).thenReturn(List.of(taggedNode));

        List<SearchService.SearchResult> results = searchService.searchByTag("java", 20);

        long distinctCount = results.stream().distinct().count();
        assertEquals(results.size(), distinctCount);
    }
}
