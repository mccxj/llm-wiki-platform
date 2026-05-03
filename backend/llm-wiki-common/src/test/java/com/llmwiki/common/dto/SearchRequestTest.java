package com.llmwiki.common.dto;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SearchRequestTest {

    @Test
    void shouldCreateWithBuilder() {
        SearchRequest request = SearchRequest.builder()
                .query("test query")
                .types(List.of("ENTITY", "CONCEPT"))
                .tags(List.of("java", "spring"))
                .limit(20)
                .offset(5)
                .build();

        assertEquals("test query", request.getQuery());
        assertEquals(List.of("ENTITY", "CONCEPT"), request.getTypes());
        assertEquals(List.of("java", "spring"), request.getTags());
        assertEquals(20, request.getLimit());
        assertEquals(5, request.getOffset());
    }

    @Test
    void shouldUseDefaultLimitAndOffset() {
        SearchRequest request = SearchRequest.builder()
                .query("test")
                .build();

        assertEquals(10, request.getLimit());
        assertEquals(0, request.getOffset());
    }

    @Test
    void shouldCreateWithNoArgs() {
        SearchRequest request = new SearchRequest();
        assertNull(request.getQuery());
        assertNull(request.getTypes());
        assertNull(request.getTags());
        assertEquals(10, request.getLimit());
        assertEquals(0, request.getOffset());
    }

    @Test
    void shouldSetFields() {
        SearchRequest request = new SearchRequest();
        request.setQuery("updated");
        request.setTypes(List.of("ENTITY"));
        request.setTags(List.of("tag1"));
        request.setLimit(5);
        request.setOffset(2);

        assertEquals("updated", request.getQuery());
        assertEquals(List.of("ENTITY"), request.getTypes());
        assertEquals(List.of("tag1"), request.getTags());
        assertEquals(5, request.getLimit());
        assertEquals(2, request.getOffset());
    }

    @Test
    void shouldCreateWithAllArgs() {
        SearchRequest request = new SearchRequest(
                "q", List.of("TYPE"), List.of("tag"), 15, 3);

        assertEquals("q", request.getQuery());
        assertEquals(List.of("TYPE"), request.getTypes());
        assertEquals(List.of("tag"), request.getTags());
        assertEquals(15, request.getLimit());
        assertEquals(3, request.getOffset());
    }
}
