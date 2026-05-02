package com.llmwiki.common.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AlignmentStatusTest {

    @Test
    void shouldHaveAllRequiredStatuses() {
        assertNotNull(AlignmentStatus.EXACT);
        assertNotNull(AlignmentStatus.FUZZY);
        assertNotNull(AlignmentStatus.GREATER);
        assertNotNull(AlignmentStatus.LESSER);
    }

    @Test
    void shouldHaveExactly4Statuses() {
        assertEquals(4, AlignmentStatus.values().length);
    }

    @Test
    void shouldMatchEnumNames() {
        assertEquals("EXACT", AlignmentStatus.EXACT.name());
        assertEquals("FUZZY", AlignmentStatus.FUZZY.name());
        assertEquals("GREATER", AlignmentStatus.GREATER.name());
        assertEquals("LESSER", AlignmentStatus.LESSER.name());
    }

    @Test
    void shouldBeSerializableToString() {
        assertEquals("EXACT", AlignmentStatus.EXACT.toString());
        assertEquals("FUZZY", AlignmentStatus.FUZZY.toString());
    }

    @Test
    void shouldBeComparableByOrdinal() {
        assertTrue(AlignmentStatus.EXACT.ordinal() < AlignmentStatus.FUZZY.ordinal());
        assertTrue(AlignmentStatus.FUZZY.ordinal() < AlignmentStatus.GREATER.ordinal());
        assertTrue(AlignmentStatus.GREATER.ordinal() < AlignmentStatus.LESSER.ordinal());
    }
}
