package com.llmwiki.domain.config.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemConfigTest {

    @Test
    void shouldCreateWithBuilder() {
        SystemConfig config = SystemConfig.builder()
                .key("scoring.threshold")
                .value("60.0")
                .description("AI scoring threshold")
                .build();

        assertEquals("scoring.threshold", config.getKey());
        assertEquals("60.0", config.getValue());
        assertEquals("AI scoring threshold", config.getDescription());
    }

    @Test
    void shouldUpdateValue() {
        SystemConfig config = SystemConfig.builder()
                .key("test.key")
                .value("old")
                .build();

        config.setValue("new");
        assertEquals("new", config.getValue());
    }
}
