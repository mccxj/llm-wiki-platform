package com.llmwiki.web.controller;

import com.llmwiki.common.dto.SearchRequest;
import com.llmwiki.service.search.SearchService;
import com.llmwiki.service.search.SearchService.AnswerResult;
import com.llmwiki.service.search.SearchService.SearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * POST /api/search — Semantic search with optional type/tag filtering.
     */
    @PostMapping("/search")
    public ResponseEntity<List<SearchResult>> search(@RequestBody SearchRequest request) {
        if (request.getQuery() == null || request.getQuery().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(searchService.search(request));
    }

    /**
     * POST /api/qa/ask — Natural language Q&A.
     */
    @PostMapping("/qa/ask")
    public ResponseEntity<AnswerResult> ask(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(searchService.ask(question));
    }

    /**
     * GET /api/search/by-tag?tag=xxx&limit=20 — Exploratory search by tag.
     */
    @GetMapping("/search/by-tag")
    public ResponseEntity<List<SearchResult>> searchByTag(@RequestParam String tag,
                                                           @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(searchService.searchByTag(tag, limit));
    }

    /**
     * GET /api/search/by-relation?nodeId=xxx&relationType=xxx&limit=20 — Exploratory search by relation.
     */
    @GetMapping("/search/by-relation")
    public ResponseEntity<List<SearchResult>> searchByRelation(@RequestParam UUID nodeId,
                                                                @RequestParam(required = false) String relationType,
                                                                @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(searchService.searchByRelation(nodeId, relationType, limit));
    }
}
