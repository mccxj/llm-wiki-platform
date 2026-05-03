package com.llmwiki.common.enums;

public enum EdgeType {
    // Generic
    RELATED_TO,
    MENTIONS,

    // Structural
    PART_OF,
    IS_A,
    EXTENDS,
    IMPLEMENTS,

    // Dependency
    DEPENDS_ON,
    USED_BY,
    CREATED_BY,

    // Evolution
    DERIVED_FROM,
    SUPERSEDES,
    COMPETES_WITH,

    // Conflict
    CONTRADICTS,

    // Similarity
    SIMILAR_TO
}
