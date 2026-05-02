package com.llmwiki.adapter.dto;

import com.llmwiki.adapter.dto.ExtractionResult.ConceptInfo;
import com.llmwiki.adapter.dto.ExtractionResult.EntityInfo;
import com.llmwiki.common.enums.AlignmentStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExtractionResultTest {

    @Test
    void entityInfo_shouldSetAndGetPositionFields() {
        EntityInfo entity = new EntityInfo("Java", "TECH", "Programming language");
        entity.setStartOffset(10);
        entity.setEndOffset(14);
        entity.setAlignmentStatus(AlignmentStatus.EXACT);
        entity.setExtractionIndex(0);

        assertEquals(10, entity.getStartOffset());
        assertEquals(14, entity.getEndOffset());
        assertEquals(AlignmentStatus.EXACT, entity.getAlignmentStatus());
        assertEquals(0, entity.getExtractionIndex());
    }

    @Test
    void entityInfo_positionFieldsShouldDefaultToNull() {
        EntityInfo entity = new EntityInfo("Java", "TECH", "Programming language");
        assertNull(entity.getStartOffset());
        assertNull(entity.getEndOffset());
        assertNull(entity.getAlignmentStatus());
        assertNull(entity.getExtractionIndex());
    }

    @Test
    void entityInfo_shouldPreserveExistingFields() {
        EntityInfo entity = new EntityInfo("Java", "TECH", "Programming language", List.of("Sun"));
        entity.setStartOffset(0);
        entity.setEndOffset(4);
        entity.setAlignmentStatus(AlignmentStatus.FUZZY);
        entity.setExtractionIndex(1);

        assertEquals("Java", entity.getName());
        assertEquals("TECH", entity.getType());
        assertEquals("Programming language", entity.getDescription());
        assertEquals(List.of("Sun"), entity.getRelatedEntities());
        assertEquals(0, entity.getStartOffset());
        assertEquals(4, entity.getEndOffset());
        assertEquals(AlignmentStatus.FUZZY, entity.getAlignmentStatus());
        assertEquals(1, entity.getExtractionIndex());
    }

    @Test
    void conceptInfo_shouldSetAndGetPositionFields() {
        ConceptInfo concept = new ConceptInfo("OOP", "Object-Oriented Programming", List.of("Java"));
        concept.setStartOffset(20);
        concept.setEndOffset(23);
        concept.setAlignmentStatus(AlignmentStatus.EXACT);
        concept.setExtractionIndex(0);

        assertEquals(20, concept.getStartOffset());
        assertEquals(23, concept.getEndOffset());
        assertEquals(AlignmentStatus.EXACT, concept.getAlignmentStatus());
        assertEquals(0, concept.getExtractionIndex());
    }

    @Test
    void conceptInfo_positionFieldsShouldDefaultToNull() {
        ConceptInfo concept = new ConceptInfo("OOP", "Object-Oriented", List.of());
        assertNull(concept.getStartOffset());
        assertNull(concept.getEndOffset());
        assertNull(concept.getAlignmentStatus());
        assertNull(concept.getExtractionIndex());
    }

    @Test
    void conceptInfo_shouldPreserveExistingFields() {
        ConceptInfo concept = new ConceptInfo("OOP", "Object-Oriented", List.of("Java"));
        concept.setStartOffset(5);
        concept.setEndOffset(8);
        concept.setAlignmentStatus(AlignmentStatus.GREATER);
        concept.setExtractionIndex(2);

        assertEquals("OOP", concept.getName());
        assertEquals("Object-Oriented", concept.getDescription());
        assertEquals(List.of("Java"), concept.getRelatedEntities());
        assertEquals(5, concept.getStartOffset());
        assertEquals(8, concept.getEndOffset());
        assertEquals(AlignmentStatus.GREATER, concept.getAlignmentStatus());
        assertEquals(2, concept.getExtractionIndex());
    }

    @Test
    void extractionResult_shouldHoldEntitiesWithPositions() {
        ExtractionResult result = new ExtractionResult();
        EntityInfo e1 = new EntityInfo("Java", "TECH", "Language");
        e1.setStartOffset(0);
        e1.setEndOffset(4);
        e1.setAlignmentStatus(AlignmentStatus.EXACT);
        e1.setExtractionIndex(0);

        EntityInfo e2 = new EntityInfo("Sun", "ORG", "Company");
        e2.setStartOffset(10);
        e2.setEndOffset(13);
        e2.setAlignmentStatus(AlignmentStatus.FUZZY);
        e2.setExtractionIndex(1);

        result.setEntities(List.of(e1, e2));

        assertEquals(2, result.getEntities().size());
        assertEquals(AlignmentStatus.EXACT, result.getEntities().get(0).getAlignmentStatus());
        assertEquals(AlignmentStatus.FUZZY, result.getEntities().get(1).getAlignmentStatus());
        assertEquals(0, result.getEntities().get(0).getExtractionIndex());
        assertEquals(1, result.getEntities().get(1).getExtractionIndex());
    }

    @Test
    void extractionResult_shouldHoldConceptsWithPositions() {
        ExtractionResult result = new ExtractionResult();
        ConceptInfo c1 = new ConceptInfo("OOP", "Paradigm", List.of("Java"));
        c1.setStartOffset(25);
        c1.setEndOffset(28);
        c1.setAlignmentStatus(AlignmentStatus.LESSER);
        c1.setExtractionIndex(0);

        result.setConcepts(List.of(c1));

        assertEquals(1, result.getConcepts().size());
        assertEquals(25, result.getConcepts().get(0).getStartOffset());
        assertEquals(28, result.getConcepts().get(0).getEndOffset());
        assertEquals(AlignmentStatus.LESSER, result.getConcepts().get(0).getAlignmentStatus());
    }

    @Test
    void entityInfo_shouldSupportAllAlignmentStatuses() {
        for (AlignmentStatus status : AlignmentStatus.values()) {
            EntityInfo entity = new EntityInfo("Test", "TECH", "desc");
            entity.setAlignmentStatus(status);
            assertEquals(status, entity.getAlignmentStatus());
        }
    }
}
