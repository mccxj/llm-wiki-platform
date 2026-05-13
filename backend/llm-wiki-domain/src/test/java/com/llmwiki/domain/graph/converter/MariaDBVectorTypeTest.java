package com.llmwiki.domain.graph.converter;

import org.junit.jupiter.api.Test;

import java.sql.Types;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MariaDBVectorType}.
 * <p>
 * These tests verify the Hibernate UserType contract methods in isolation.
 * Full integration with MariaDB is covered by {@code MariaDbVectorIntegrationTest}
 * (requires Docker).
 */
class MariaDBVectorTypeTest {

    private final MariaDBVectorType type = new MariaDBVectorType();

    @Test
    void returnedClass_shouldBeFloatArray() {
        assertEquals(float[].class, type.returnedClass());
    }

    @Test
    void getSqlType_shouldBeOther() {
        assertEquals(Types.OTHER, type.getSqlType());
    }

    @Test
    void equals_identicalArrays_returnsTrue() {
        float[] a = {1.0f, 2.0f, 3.0f};
        float[] b = {1.0f, 2.0f, 3.0f};
        assertTrue(type.equals(a, b));
    }

    @Test
    void equals_differentArrays_returnsFalse() {
        float[] a = {1.0f, 2.0f, 3.0f};
        float[] b = {4.0f, 5.0f, 6.0f};
        assertFalse(type.equals(a, b));
    }

    @Test
    void equals_bothNull_returnsTrue() {
        assertTrue(type.equals(null, null));
    }

    @Test
    void equals_oneNull_returnsFalse() {
        float[] a = {1.0f};
        assertFalse(type.equals(a, null));
        assertFalse(type.equals(null, a));
    }

    @Test
    void hashCode_shouldBeConsistentWithArraysHashCode() {
        float[] a = {1.0f, 2.0f, 3.0f};
        assertEquals(java.util.Arrays.hashCode(a), type.hashCode(a));
    }

    @Test
    void deepCopy_shouldReturnIndependentCopy() {
        float[] original = {1.0f, 2.0f, 3.0f};
        float[] copy = type.deepCopy(original);
        assertArrayEquals(original, copy);
        assertNotSame(original, copy); // must be a different object
    }

    @Test
    void deepCopy_null_returnsNull() {
        assertNull(type.deepCopy(null));
    }

    @Test
    void isMutable_shouldBeTrue() {
        assertTrue(type.isMutable());
    }

    @Test
    void disassemble_shouldReturnSameArray() {
        float[] input = {0.1f, 0.2f};
        java.io.Serializable result = type.disassemble(input);
        assertSame(input, result);
    }

    @Test
    void assemble_shouldReturnSameArray() {
        float[] input = {0.1f, 0.2f};
        float[] result = type.assemble(input, null);
        assertSame(input, result);
    }

    @Test
    void replace_shouldReturnDeepCopy() {
        float[] detached = {1.0f, 2.0f};
        float[] managed = {3.0f, 4.0f};
        float[] result = type.replace(detached, managed, null);
        assertArrayEquals(detached, result);
        assertNotSame(detached, result);
    }
}
