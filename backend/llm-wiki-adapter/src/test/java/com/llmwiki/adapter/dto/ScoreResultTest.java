package com.llmwiki.adapter.dto;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ScoreResultTest {

    @Test
    void shouldCreateScoreResult() {
        ScoreResult result = new ScoreResult();
        result.setOverallScore(new BigDecimal("7.5"));
        result.setReason("Good document");
        result.setScores(Map.of("information_density", 8, "entity_richness", 7));
        result.setKeyEntities(List.of("Entity1", "Entity2"));
        result.setSuggestedTags(List.of("tag1", "tag2"));

        assertEquals(new BigDecimal("7.5"), result.getOverallScore());
        assertEquals("Good document", result.getReason());
        assertEquals(2, result.getKeyEntities().size());
        assertEquals(2, result.getSuggestedTags().size());
    }

    @Test
    void shouldCreateExtractionResult() {
        ExtractionResult.EntityInfo entity = new ExtractionResult.EntityInfo(
            "Java", "Programming Language", "A programming language");
        assertEquals("Java", entity.getName());
        assertEquals("Programming Language", entity.getType());

        ExtractionResult.ConceptInfo concept = new ExtractionResult.ConceptInfo(
            "OOP", "Object-Oriented Programming", List.of("Java", "C++"));
        assertEquals("OOP", concept.getName());
        assertEquals(2, concept.getRelatedEntities().size());
    }
}
