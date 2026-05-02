package com.llmwiki.adapter.prompting;

import com.llmwiki.adapter.dto.ExampleData;
import com.llmwiki.adapter.dto.ExampleData.LabeledExtraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PromptTemplateTest {

    private PromptTemplate template;

    @BeforeEach
    void setUp() {
        template = new PromptTemplate("Extract named entities from the text.", List.of());
    }

    @Test
    void render_shouldReturnDescriptionOnlyWhenNoExamples() {
        String result = template.render("Java is a programming language.");
        assertTrue(result.contains("Extract named entities from the text."));
        assertTrue(result.contains("Java is a programming language."));
        assertFalse(result.contains("Example 1"));
    }

    @Test
    void render_shouldIncludeExamplesWhenProvided() {
        List<ExampleData> examples = List.of(
                new ExampleData("Albert Einstein was a German physicist.",
                        List.of(new LabeledExtraction("PERSON", "Albert Einstein", "Physicist", null))));
        PromptTemplate templateWithExamples = new PromptTemplate("Extract entities.", examples);

        String result = templateWithExamples.render("New text to process.");

        assertTrue(result.contains("Extract entities."));
        assertTrue(result.contains("Example 1"));
        assertTrue(result.contains("Albert Einstein was a German physicist."));
        assertTrue(result.contains("New text to process."));
        assertTrue(result.contains("Albert Einstein"));
        assertTrue(result.contains("PERSON"));
    }

    @Test
    void render_shouldIncludeMultipleExamples() {
        List<ExampleData> examples = List.of(
                new ExampleData("Text one.",
                        List.of(new LabeledExtraction("PERSON", "Einstein", "Physicist", null))),
                new ExampleData("Text two.",
                        List.of(new LabeledExtraction("ORG", "MIT", "University", null))));
        PromptTemplate templateWithExamples = new PromptTemplate("Extract.", examples);

        String result = templateWithExamples.render("Input text.");

        assertTrue(result.contains("Example 1"));
        assertTrue(result.contains("Text one."));
        assertTrue(result.contains("Einstein"));
        assertTrue(result.contains("Example 2"));
        assertTrue(result.contains("Text two."));
        assertTrue(result.contains("MIT"));
        assertTrue(result.contains("Input text."));
    }

    @Test
    void render_shouldIncludeExtractionTextAndClass() {
        List<ExampleData> examples = List.of(
                new ExampleData("Sample text.",
                        List.of(new LabeledExtraction("TECH", "Java", "Programming language",
                                List.of("object-oriented", "JVM")))));
        PromptTemplate templateWithExamples = new PromptTemplate("Extract.", examples);

        String result = templateWithExamples.render("Target text.");

        assertTrue(result.contains("TECH"));
        assertTrue(result.contains("Java"));
        assertTrue(result.contains("Programming language"));
        assertTrue(result.contains("object-oriented"));
        assertTrue(result.contains("JVM"));
    }

    @Test
    void render_shouldFormatExamplesInOrder() {
        List<ExampleData> examples = List.of(
                new ExampleData("First.", List.of(new LabeledExtraction("A", "a1", "d1", null))),
                new ExampleData("Second.", List.of(new LabeledExtraction("B", "b1", "d2", null))),
                new ExampleData("Third.", List.of(new LabeledExtraction("C", "c1", "d3", null))));
        PromptTemplate templateWithExamples = new PromptTemplate("Extract.", examples);

        String result = templateWithExamples.render("Input.");

        int pos1 = result.indexOf("Example 1");
        int pos2 = result.indexOf("Example 2");
        int pos3 = result.indexOf("Example 3");
        assertTrue(pos1 < pos2);
        assertTrue(pos2 < pos3);
    }

    @Test
    void render_shouldHandleEmptyInputText() {
        String result = template.render("");
        assertTrue(result.contains("Extract named entities from the text."));
    }

    @Test
    void render_shouldHandleMultipleExtractionsInOneExample() {
        List<ExampleData> examples = List.of(
                new ExampleData("Einstein worked at Princeton.",
                        List.of(
                                new LabeledExtraction("PERSON", "Einstein", "Physicist", null),
                                new LabeledExtraction("ORG", "Princeton", "University", null))));
        PromptTemplate templateWithExamples = new PromptTemplate("Extract.", examples);

        String result = templateWithExamples.render("New text.");

        assertTrue(result.contains("Einstein"));
        assertTrue(result.contains("Princeton"));
        assertTrue(result.contains("PERSON"));
        assertTrue(result.contains("ORG"));
    }

    @Test
    void constructor_shouldThrowOnNullDescription() {
        assertThrows(NullPointerException.class,
                () -> new PromptTemplate(null, List.of()));
    }

    @Test
    void constructor_shouldThrowOnEmptyDescription() {
        assertThrows(IllegalArgumentException.class,
                () -> new PromptTemplate("", List.of()));
    }

    @Test
    void constructor_shouldThrowOnNullExamples() {
        assertThrows(NullPointerException.class,
                () -> new PromptTemplate("Extract entities.", null));
    }
}
