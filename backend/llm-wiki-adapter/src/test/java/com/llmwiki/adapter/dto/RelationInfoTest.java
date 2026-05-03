package com.llmwiki.adapter.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RelationInfoTest {

    @Test
    void shouldCreateWithConstructor() {
        RelationInfo r = new RelationInfo("Java", "Spring", "DEPENDS_ON", 0.95);
        assertEquals("Java", r.getSourceEntity());
        assertEquals("Spring", r.getTargetEntity());
        assertEquals("DEPENDS_ON", r.getRelationType());
        assertEquals(0.95, r.getConfidence());
    }

    @Test
    void shouldCreateWithSetters() {
        RelationInfo r = new RelationInfo();
        r.setSourceEntity("Java");
        r.setTargetEntity("OOP");
        r.setRelationType("IS_A");
        r.setConfidence(0.88);
        assertEquals("Java", r.getSourceEntity());
        assertEquals("OOP", r.getTargetEntity());
        assertEquals("IS_A", r.getRelationType());
        assertEquals(0.88, r.getConfidence());
    }

    @Test
    void isConfident_shouldReturnTrueWhenAboveThreshold() {
        RelationInfo r = new RelationInfo("A", "B", "RELATED_TO", 0.90);
        assertTrue(r.isConfident(0.85));
        assertTrue(r.isConfident(0.90));
    }

    @Test
    void isConfident_shouldReturnFalseWhenBelowThreshold() {
        RelationInfo r = new RelationInfo("A", "B", "RELATED_TO", 0.80);
        assertFalse(r.isConfident(0.85));
    }

    @Test
    void isConfident_shouldReturnFalseWhenConfidenceIsNull() {
        RelationInfo r = new RelationInfo("A", "B", "RELATED_TO", null);
        assertFalse(r.isConfident(0.50));
    }

    @Test
    void hasValidType_shouldReturnTrueForNonBlankType() {
        assertTrue(new RelationInfo("A", "B", "DEPENDS_ON", 0.9).hasValidType());
    }

    @Test
    void hasValidType_shouldReturnFalseForNullOrBlank() {
        assertFalse(new RelationInfo("A", "B", null, 0.9).hasValidType());
        assertFalse(new RelationInfo("A", "B", "", 0.9).hasValidType());
        assertFalse(new RelationInfo("A", "B", "  ", 0.9).hasValidType());
    }
}
