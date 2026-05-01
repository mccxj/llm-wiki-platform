package com.llmwiki.web.controller;

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
        when(searchService.search("java", 10)).thenReturn(expected);

        var response = controller.search("java", 10);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals("java", response.getBody().get(0).pageSlug);
    }

    @Test
    void search_shouldUseDefaultLimit() {
        when(searchService.search("test", 10)).thenReturn(List.of());

        controller.search("test", 10);

        verify(searchService).search("test", 10);
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
}
