package com.llmwiki.adapter.prompting;

import com.llmwiki.adapter.dto.ExampleData;
import com.llmwiki.adapter.dto.ExampleData.LabeledExtraction;

import java.util.List;
import java.util.Objects;

/**
 * Renders few-shot prompts following LangExtract's prompt pattern.
 * Format: description + formatted_examples + input_text
 */
public class PromptTemplate {
    private final String description;
    private final List<ExampleData> examples;

    public PromptTemplate(String description, List<ExampleData> examples) {
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.examples = Objects.requireNonNull(examples, "examples must not be null");
        if (description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
    }

    /**
     * Render the full prompt with description, examples, and input text.
     */
    public String render(String inputText) {
        StringBuilder sb = new StringBuilder();
        sb.append(description).append("\n\n");

        if (!examples.isEmpty()) {
            for (int i = 0; i < examples.size(); i++) {
                ExampleData example = examples.get(i);
                sb.append("Example ").append(i + 1).append(":\n");
                sb.append("Text: ").append(example.getText()).append("\n");
                sb.append("Extractions:\n");
                for (LabeledExtraction ext : example.getExtractions()) {
                    sb.append("- ").append(ext.getExtractionClass())
                            .append(": ").append(ext.getExtractionText());
                    if (ext.getDescription() != null && !ext.getDescription().isEmpty()) {
                        sb.append(" (").append(ext.getDescription()).append(")");
                    }
                    if (ext.getAttributes() != null && !ext.getAttributes().isEmpty()) {
                        sb.append(" [").append(String.join(", ", ext.getAttributes())).append("]");
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
        }

        sb.append("Text: ").append(inputText).append("\n");
        sb.append("Extractions:\n");

        return sb.toString();
    }

    public String getDescription() {
        return description;
    }

    public List<ExampleData> getExamples() {
        return examples;
    }
}
