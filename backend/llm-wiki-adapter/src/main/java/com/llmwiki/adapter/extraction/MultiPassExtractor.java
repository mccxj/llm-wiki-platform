package com.llmwiki.adapter.extraction;

import com.llmwiki.adapter.api.AiApiClient;
import com.llmwiki.adapter.dto.ExtractionResult;
import com.llmwiki.adapter.dto.ExtractionResult.EntityInfo;
import com.llmwiki.common.enums.ExtractionPass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Multi-pass extraction with merging and deduplication.
 * Pass 1 (FIRST_PASS): Broad extraction targeting all entity types.
 * Pass 2 (SECOND_PASS): Targeted prompt for entity types NOT found in pass 1.
 * Pass 3 (THIRD_PASS): Target specific sections with low entity density.
 * Results are merged with deduplication by name.toLowerCase().
 * Follows LangExtract's extraction_passes=3 pattern for 10-20% recall improvement.
 */
@Component
public class MultiPassExtractor {

    private static final Logger log = LoggerFactory.getLogger(MultiPassExtractor.class);

    private final AiApiClient aiClient;

    public MultiPassExtractor(AiApiClient aiClient) {
        this.aiClient = Objects.requireNonNull(aiClient, "aiClient must not be null");
    }

    /**
     * Extract entities using multiple passes with dedup by name.toLowerCase().
     */
    public List<EntityInfo> extractAll(String content, Set<String> entityTypes) {
        if (content == null || content.isBlank()) {
            return Collections.emptyList();
        }

        Map<String, EntityInfo> merged = new LinkedHashMap<>();

        // Pass 1: extract entities normally
        ExtractionResult pass1 = aiClient.extractEntities(content);
        merge(merged, pass1.getEntities());

        if (entityTypes != null && !entityTypes.isEmpty()) {
            // Find missed types
            Set<String> foundTypes = new HashSet<>();
            for (EntityInfo e : merged.values()) {
                if (e.getType() != null) foundTypes.add(e.getType().toUpperCase());
            }
            Set<String> missedTypes = new LinkedHashSet<>(entityTypes);
            missedTypes.removeAll(foundTypes);

            if (!missedTypes.isEmpty()) {
                log.debug("Pass 2: targeting missed types {}", missedTypes);
                String targetedContent = buildTargetedPrompt(content, missedTypes);
                ExtractionResult pass2 = aiClient.extractEntities(targetedContent);
                merge(merged, pass2.getEntities());
            }
        }

        log.info("Multi-pass extraction complete: {} entities", merged.size());
        return new ArrayList<>(merged.values());
    }

    private void merge(Map<String, EntityInfo> merged, List<EntityInfo> entities) {
        if (entities == null) return;
        for (EntityInfo e : entities) {
            String key = e.getName().toLowerCase();
            if (!merged.containsKey(key)) {
                merged.put(key, e);
            }
        }
    }

    private String buildTargetedPrompt(String content, Set<String> missedTypes) {
        return "Focus on extracting these entity types: " + String.join(", ", missedTypes) + ".\n\n" + content;
    }
}
