package com.llmwiki.adapter.dto;

import java.util.List;

/**
 * Represents a few-shot example with text and labeled extractions.
 * Mirrors LangExtract's ExampleData pattern.
 */
public class ExampleData {
    private final String text;
    private final List<LabeledExtraction> extractions;

    public ExampleData(String text, List<LabeledExtraction> extractions) {
        this.text = text;
        this.extractions = extractions;
    }

    public String getText() {
        return text;
    }

    public List<LabeledExtraction> getExtractions() {
        return extractions;
    }

    /**
     * A single labeled extraction within an example.
     */
    public static class LabeledExtraction {
        private final String extractionClass;
        private final String extractionText;
        private final String description;
        private final List<String> attributes;

        public LabeledExtraction(String extractionClass, String extractionText,
                                 String description, List<String> attributes) {
            this.extractionClass = extractionClass;
            this.extractionText = extractionText;
            this.description = description;
            this.attributes = attributes;
        }

        public String getExtractionClass() {
            return extractionClass;
        }

        public String getExtractionText() {
            return extractionText;
        }

        public String getDescription() {
            return description;
        }

        public List<String> getAttributes() {
            return attributes;
        }
    }
}
