package com.llmwiki.adapter.api;

import com.llmwiki.adapter.dto.ExampleData;
import com.llmwiki.adapter.dto.ExampleData.LabeledExtraction;
import com.llmwiki.adapter.dto.ExtractionResult;
import com.llmwiki.adapter.resolver.AlignmentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiApiClientFewShotTest {

    OpenAiApiClient client;

    @BeforeEach
    void setUp() {
        client = new OpenAiApiClient("http://localhost:9999", "test-key", "test-model", "", "", "", new AlignmentResolver());
    }

    @Test
    void extractEntities_withExamples_shouldReturnEmptyForUnreachableServer() {
        List<ExampleData> examples = List.of(
                new ExampleData("Einstein was a physicist.",
                        List.of(new LabeledExtraction("PERSON", "Einstein", "Physicist", null))));

        ExtractionResult result = client.extractEntities("Test content", examples);
        assertNotNull(result);
        assertTrue(result.getEntities() == null || result.getEntities().isEmpty());
    }

    @Test
    void extractConcepts_withExamples_shouldReturnEmptyForUnreachableServer() {
        List<ExampleData> examples = List.of(
                new ExampleData("OOP is important.",
                        List.of(new LabeledExtraction("CONCEPT", "OOP", "Object-oriented", null))));

        ExtractionResult result = client.extractConcepts("Test content", examples);
        assertNotNull(result);
        assertTrue(result.getConcepts() == null || result.getConcepts().isEmpty());
    }

    @Test
    void extractEntities_withEmptyExamples_shouldReturnEmptyForUnreachableServer() {
        ExtractionResult result = client.extractEntities("Test content", List.of());
        assertNotNull(result);
        assertTrue(result.getEntities() == null || result.getEntities().isEmpty());
    }

    @Test
    void extractEntities_withNullExamples_shouldReturnEmptyForUnreachableServer() {
        ExtractionResult result = client.extractEntities("Test content", null);
        assertNotNull(result);
        assertTrue(result.getEntities() == null || result.getEntities().isEmpty());
    }

    @Test
    void constructor_shouldAcceptExamplesParameter() {
        // The few-shot overloaded methods should not break existing constructor
        assertNotNull(client);
        assertTrue(client.isAvailable());
    }
}
