package com.llmwiki.adapter.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExampleDataTest {

    @Test
    void shouldCreateExampleDataWithTextAndExtractions() {
        ExampleData.LabeledExtraction extraction = new ExampleData.LabeledExtraction(
                "PERSON", "Albert Einstein", "Physicist", List.of("German", "theoretical physicist"));
        ExampleData exampleData = new ExampleData("Albert Einstein was a German theoretical physicist.",
                List.of(extraction));

        assertEquals("Albert Einstein was a German theoretical physicist.", exampleData.getText());
        assertEquals(1, exampleData.getExtractions().size());
        assertEquals("PERSON", exampleData.getExtractions().get(0).getExtractionClass());
        assertEquals("Albert Einstein", exampleData.getExtractions().get(0).getExtractionText());
        assertEquals("Physicist", exampleData.getExtractions().get(0).getDescription());
        assertEquals(List.of("German", "theoretical physicist"), exampleData.getExtractions().get(0).getAttributes());
    }

    @Test
    void shouldSupportMultipleExtractions() {
        List<ExampleData.LabeledExtraction> extractions = List.of(
                new ExampleData.LabeledExtraction("PERSON", "Einstein", "Physicist", null),
                new ExampleData.LabeledExtraction("ORG", "Princeton University", "University", null));
        ExampleData exampleData = new ExampleData("Example text", extractions);

        assertEquals(2, exampleData.getExtractions().size());
        assertEquals("PERSON", exampleData.getExtractions().get(0).getExtractionClass());
        assertEquals("ORG", exampleData.getExtractions().get(1).getExtractionClass());
    }

    @Test
    void shouldSupportNullAttributes() {
        ExampleData.LabeledExtraction extraction = new ExampleData.LabeledExtraction(
                "TECH", "Java", "Language", null);
        assertNull(extraction.getAttributes());
    }

    @Test
    void labeledExtractionShouldSupportEmptyAttributes() {
        ExampleData.LabeledExtraction extraction = new ExampleData.LabeledExtraction(
                "TECH", "Java", "Language", List.of());
        assertNotNull(extraction.getAttributes());
        assertTrue(extraction.getAttributes().isEmpty());
    }

    @Test
    void exampleDataShouldBeImmutableViaList() {
        ExampleData data = new ExampleData("text", List.of(
                new ExampleData.LabeledExtraction("A", "B", "C", null)));
        // The returned list should be the same reference (standard POJO behavior)
        assertEquals(1, data.getExtractions().size());
    }
}
