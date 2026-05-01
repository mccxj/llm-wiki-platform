package com.llmwiki.web.controller;

import com.llmwiki.common.dto.SearchRequest;
import com.llmwiki.service.search.SearchService;
import com.llmwiki.service.search.SearchService.AnswerResult;
import com.llmwiki.service.search.SearchService.SearchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    @Mock SearchService searchService;
    @InjectMocks SearchController controller;

    @Test
    void search_shouldReturnResults() {
        SearchResult sr = new SearchResult();
        sr.pageSlug = "java";
        List<SearchResult> expected = List.of(sr);
        SearchRequest request = SearchRequest.builder().query("java").limit(10).build();
        when(searchService.search(request)).thenReturn(expected);

        var response = controller.search(request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals("java", response.getBody().get(0).pageSlug);
    }

    @Test
    void search_shouldReturn400ForBlankQuery() {
        SearchRequest request = SearchRequest.builder().query("").build();

        var response = controller.search(request);

        assertEquals(400, response.getStatusCodeValue());
        verify(searchService, never()).search(any(SearchRequest.class));
    }

    @Test
    void search_shouldReturn400ForNullQuery() {
        SearchRequest request = SearchRequest.builder().build();

        var response = controller.search(request);

        assertEquals(400, response.getStatusCodeValue());
        verify(searchService, never()).search(any(SearchRequest.class));
    }

    @Test
    void search_shouldPassTypesAndTags() {
        SearchResult sr = new SearchResult();
        sr.nodeType = "ENTITY";
        List<SearchResult> expected = List.of(sr);
        SearchRequest request = SearchRequest.builder()
                .query("java")
                .types(List.of("ENTITY"))
                .tags(List.of("programming"))
                .limit(5)
                .offset(0)
                .build();
        when(searchService.search(request)).thenReturn(expected);

        var response = controller.search(request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        verify(searchService).search(request);
    }

    @Test
    void ask_shouldReturnAnswer() {
        AnswerResult expected = new AnswerResult("Java is a language.", "KNOWLEDGE_BASE", List.of("Java Page"));
        when(searchService.ask("What is Java?")).thenReturn(expected);

        var response = controller.ask(Map.of("question", "What is Java?"));

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Java is a language.", response.getBody().answer);
        assertEquals("KNOWLEDGE_BASE", response.getBody().source);
    }

    @Test
    void ask_shouldReturn400ForBlankQuestion() {
        var response = controller.ask(Map.of("question", ""));

        assertEquals(400, response.getStatusCodeValue());
        verify(searchService, never()).ask(any());
    }

    @Test
    void ask_shouldReturn400ForNullQuestion() {
        var response = controller.ask(Map.of());

        assertEquals(400, response.getStatusCodeValue());
        verify(searchService, never()).ask(any());
    }

    @Test
    void searchByTag_shouldReturnResults() {
        SearchResult sr = new SearchResult();
        sr.nodeName = "Java";
        List<SearchResult> expected = List.of(sr);
        when(searchService.searchByTag("java", 20)).thenReturn(expected);

        var response = controller.searchByTag("java", 20);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals("Java", response.getBody().get(0).nodeName);
    }

    @Test
    void searchByRelation_shouldReturnResults() {
        SearchResult sr = new SearchResult();
        sr.nodeName = "Related";
        List<SearchResult> expected = List.of(sr);
        UUID nodeId = UUID.randomUUID();
        when(searchService.searchByRelation(nodeId, "RELATED_TO", 20)).thenReturn(expected);

        var response = controller.searchByRelation(nodeId, "RELATED_TO", 20);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals("Related", response.getBody().get(0).nodeName);
    }

    @Test
    void searchByRelation_shouldWorkWithNullRelationType() {
        SearchResult sr = new SearchResult();
        sr.nodeName = "AnyRelation";
        List<SearchResult> expected = List.of(sr);
        UUID nodeId = UUID.randomUUID();
        when(searchService.searchByRelation(nodeId, null, 20)).thenReturn(expected);

        var response = controller.searchByRelation(nodeId, null, 20);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }
}
