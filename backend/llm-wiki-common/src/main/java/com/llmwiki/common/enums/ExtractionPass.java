package com.llmwiki.common.enums;

/**
 * Represents the extraction pass in a multi-pass extraction strategy.
 * <p>
 * FIRST_PASS:  Broad extraction targeting all entity types with low temperature.
 * SECOND_PASS: Targeted prompt asking for entity types NOT found in first pass.
 * THIRD_PASS:  Target specific sections with low entity density.
 * <p>
 * LangExtract uses extraction_passes=3 for 10-20% recall improvement on long documents.
 */
public enum ExtractionPass {
    /**
     * Pass 1: First pass — broad extraction for all entity types.
     */
    FIRST_PASS,
    /**
     * Pass 2: Second pass — target entity types missed in first pass.
     */
    SECOND_PASS,
    /**
     * Pass 3: Third pass — target specific sections with low entity density.
     */
    THIRD_PASS
}
