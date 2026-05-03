package com.llmwiki.adapter.dto;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class UnifiedExtractionResultTest {

    @Test
    void shouldCreateUnifiedExtractionResult() {
        UnifiedExtractionResult result = new UnifiedExtractionResult();

        UnifiedExtractionResult.EntityInfo entity = new UnifiedExtractionResult.EntityInfo(
            "Java", "TECH", "A programming language", List.of("JVM"));
        UnifiedExtractionResult.ConceptInfo concept = new UnifiedExtractionResult.ConceptInfo(
            "OOP", "Object-Oriented Programming", List.of("Java"));
        UnifiedExtractionResult.RelationInfo relation = new UnifiedExtractionResult.RelationInfo(
            "Java", "JVM", "RUNS_ON", 0.95);

        result.setEntities(List.of(entity));
        result.setConcepts(List.of(concept));
        result.setRelations(List.of(relation));

        assertEquals(1, result.getEntities().size());
        assertEquals(1, result.getConcepts().size());
        assertEquals(1, result.getRelations().size());
        assertEquals("Java", result.getEntities().get(0).getName());
        assertEquals("OOP", result.getConcepts().get(0).getName());
        assertEquals("RUNS_ON", result.getRelations().get(0).getRelationType());
        assertEquals(0.95, result.getRelations().get(0).getConfidence(), 0.01);
    }

    @Test
    void shouldCreateRelationInfo() {
        UnifiedExtractionResult.RelationInfo relation = new UnifiedExtractionResult.RelationInfo(
            "Python", "Django", "USES", 0.88);

        assertEquals("Python", relation.getSourceEntity());
        assertEquals("Django", relation.getTargetEntity());
        assertEquals("USES", relation.getRelationType());
        assertEquals(0.88, relation.getConfidence(), 0.01);
    }

    @Test
    void shouldHandleEmptyLists() {
        UnifiedExtractionResult result = new UnifiedExtractionResult();
        result.setEntities(List.of());
        result.setConcepts(List.of());
        result.setRelations(List.of());

        assertTrue(result.getEntities().isEmpty());
        assertTrue(result.getConcepts().isEmpty());
        assertTrue(result.getRelations().isEmpty());
    }
}
