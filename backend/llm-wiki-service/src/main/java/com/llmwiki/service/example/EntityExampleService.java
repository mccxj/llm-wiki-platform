package com.llmwiki.service.example;

import com.llmwiki.adapter.dto.ExampleData;
import com.llmwiki.adapter.dto.ExampleData.LabeledExtraction;
import com.llmwiki.domain.example.entity.EntityExample;
import com.llmwiki.domain.example.repository.EntityExampleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntityExampleService {

    private final EntityExampleRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public EntityExample createExample(String name, String entityType, String exampleText, String extractionData) {
        EntityExample example = EntityExample.builder()
                .name(name)
                .entityType(entityType)
                .exampleText(exampleText)
                .extractionData(extractionData)
                .build();
        return repository.save(example);
    }

    @Transactional
    public EntityExample updateExample(UUID id, String name, String entityType, String exampleText, String extractionData) {
        EntityExample example = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("EntityExample not found: " + id));
        example.setName(name);
        example.setEntityType(entityType);
        example.setExampleText(exampleText);
        example.setExtractionData(extractionData);
        return repository.save(example);
    }

    @Transactional
    public void deleteExample(UUID id) {
        EntityExample example = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("EntityExample not found: " + id));
        example.setDeleted(true);
        repository.save(example);
    }

    public EntityExample findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("EntityExample not found: " + id));
    }

    public List<EntityExample> findAll() {
        return repository.findAllActive();
    }

    public List<EntityExample> findByType(String entityType) {
        return repository.findByEntityTypeAndDeletedFalse(entityType);
    }

    /**
     * Convert entity examples to ExampleData for few-shot prompting.
     * Uses Jackson ObjectMapper for robust JSON parsing.
     */
    public List<ExampleData> loadExamplesAsExampleData(String entityType) {
        List<EntityExample> examples = repository.findByEntityTypeAndDeletedFalse(entityType);
        List<ExampleData> result = new ArrayList<>();

        for (EntityExample example : examples) {
            String extractionJson = example.getExtractionData();
            if (extractionJson == null || extractionJson.isBlank() || extractionJson.equals("[]")) {
                result.add(new ExampleData(example.getExampleText(), Collections.emptyList()));
                continue;
            }

            try {
                List<LabeledExtraction> extractions = parseExtractions(extractionJson);
                result.add(new ExampleData(example.getExampleText(), extractions));
            } catch (Exception e) {
                log.warn("Failed to parse extraction data for example {}: {}", example.getId(), e.getMessage());
                result.add(new ExampleData(example.getExampleText(), Collections.emptyList()));
            }
        }

        return result;
    }

    /**
     * Parse extraction JSON array into LabeledExtraction objects.
     * Expected format: [{"extractionClass":"TECH","extractionText":"Java","description":"...","attributes":["OOP"]}]
     */
    private List<LabeledExtraction> parseExtractions(String json) throws Exception {
        List<ExtractionJsonItem> items = objectMapper.readValue(json, new TypeReference<List<ExtractionJsonItem>>() {});
        List<LabeledExtraction> result = new ArrayList<>();
        for (ExtractionJsonItem item : items) {
            result.add(new LabeledExtraction(
                    item.extractionClass,
                    item.extractionText,
                    item.description,
                    item.attributes != null ? item.attributes : Collections.emptyList()
            ));
        }
        return result;
    }

    /**
     * Internal DTO for Jackson deserialization of extraction JSON.
     */
    private static class ExtractionJsonItem {
        public String extractionClass;
        public String extractionText;
        public String description;
        public List<String> attributes;
    }
}
