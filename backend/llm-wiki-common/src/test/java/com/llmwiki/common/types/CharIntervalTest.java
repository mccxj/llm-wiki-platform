package com.llmwiki.common.types;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CharIntervalTest {

    @Test
    void shouldCreateIntervalWithStartAndEnd() {
        CharInterval interval = new CharInterval(10, 20);
        assertEquals(10, interval.getStartOffset());
        assertEquals(20, interval.getEndOffset());
    }

    @Test
    void shouldSetStartAndEndOffsets() {
        CharInterval interval = new CharInterval();
        interval.setStartOffset(5);
        interval.setEndOffset(15);
        assertEquals(5, interval.getStartOffset());
        assertEquals(15, interval.getEndOffset());
    }

    @Test
    void shouldCalculateLength() {
        CharInterval interval = new CharInterval(10, 20);
        assertEquals(10, interval.length());
    }

    @Test
    void shouldBeEqualWhenSameOffsets() {
        CharInterval a = new CharInterval(10, 20);
        CharInterval b = new CharInterval(10, 20);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenDifferentOffsets() {
        CharInterval a = new CharInterval(10, 20);
        CharInterval b = new CharInterval(15, 25);
        assertNotEquals(a, b);
    }

    @Test
    void shouldNotBeEqualWhenNull() {
        CharInterval interval = new CharInterval(10, 20);
        assertNotEquals(null, interval);
    }

    @Test
    void shouldNotBeEqualWhenDifferentClass() {
        CharInterval interval = new CharInterval(10, 20);
        assertNotEquals("string", interval);
    }

    @Test
    void shouldHaveCorrectToString() {
        CharInterval interval = new CharInterval(10, 20);
        String str = interval.toString();
        assertTrue(str.contains("startOffset=10"));
        assertTrue(str.contains("endOffset=20"));
    }
}
