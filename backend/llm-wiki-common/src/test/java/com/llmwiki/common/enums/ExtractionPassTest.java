package com.llmwiki.common.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExtractionPassTest {

    @Test
    void shouldHaveFirstPass() {
        assertEquals("FIRST_PASS", ExtractionPass.FIRST_PASS.name());
    }

    @Test
    void shouldHaveSecondPass() {
        assertEquals("SECOND_PASS", ExtractionPass.SECOND_PASS.name());
    }

    @Test
    void shouldHaveThirdPass() {
        assertEquals("THIRD_PASS", ExtractionPass.THIRD_PASS.name());
    }

    @Test
    void shouldHaveThreeValues() {
        assertEquals(3, ExtractionPass.values().length);
    }
}
