package com.llmwiki.web.controller;

import com.llmwiki.service.search.SearchService;
import com.llmwiki.service.search.SearchService.AnswerResult;
import com.llmwiki.service.search.SearchService.SearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/search")
    public ResponseEntity<List<SearchResult>> search(@RequestParam String q,
                                                      @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(searchService.search(q, limit));
    }

    @PostMapping("/ask")
    public ResponseEntity<AnswerResult> ask(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(searchService.ask(question));
    }
}
