package com.llmwiki.domain.graph.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FloatArrayToJsonConverterTest {

    private FloatArrayToJsonConverter converter;

    @BeforeEach
    void setUp() {
        converter = new FloatArrayToJsonConverter();
    }

    @Test
    void shouldConvertFloatArrayToJsonString() {
        float[] input = {0.1f, 0.2f, 0.3f};
        String result = converter.convertToDatabaseColumn(input);
        assertEquals("[0.1,0.2,0.3]", result);
    }

    @Test
    void shouldConvertJsonStringToFloatArray() {
        String input = "[0.1,0.2,0.3]";
        float[] result = converter.convertToEntityAttribute(input);
        assertEquals(3, result.length);
        assertEquals(0.1f, result[0], 0.0001f);
        assertEquals(0.2f, result[1], 0.0001f);
        assertEquals(0.3f, result[2], 0.0001f);
    }

    @Test
    void shouldHandleNullInput() {
        String result = converter.convertToDatabaseColumn(null);
        assertEquals("[]", result);
        
        float[] arrayResult = converter.convertToEntityAttribute(null);
        assertEquals(0, arrayResult.length);
    }

    @Test
    void shouldHandleEmptyArray() {
        float[] empty = new float[0];
        String result = converter.convertToDatabaseColumn(empty);
        assertEquals("[]", result);
    }

    @Test
    void shouldHandleEmptyJsonString() {
        float[] result = converter.convertToEntityAttribute("");
        assertEquals(0, result.length);
    }

    @Test
    void shouldHandleEmptyJsonArray() {
        float[] result = converter.convertToEntityAttribute("[]");
        assertEquals(0, result.length);
    }

    @Test
    void shouldHandleSingleElement() {
        float[] toDb = {1.5f};
        String json = converter.convertToDatabaseColumn(toDb);
        assertEquals("[1.5]", json);
        
        float[] fromDb = converter.convertToEntityAttribute(json);
        assertEquals(1, fromDb.length);
        assertEquals(1.5f, fromDb[0], 0.0001f);
    }

    @Test
    void shouldRoundTrip() {
        float[] original = {0.1f, 0.25f, 0.333f, 0.5f, 1.0f};
        String json = converter.convertToDatabaseColumn(original);
        float[] result = converter.convertToEntityAttribute(json);
        
        assertArrayEquals(original, result, 0.0001f);
    }

    @Test
    void shouldThrowOnInvalidJson() {
        assertThrows(IllegalArgumentException.class, () -> {
            converter.convertToEntityAttribute("not-a-json-array");
        });
    }

    @Test
    void shouldHandleNegativeValues() {
        float[] negative = {-1.5f, 0.0f, 1.5f};
        String json = converter.convertToDatabaseColumn(negative);
        assertEquals("[-1.5,0.0,1.5]", json);
        
        float[] result = converter.convertToEntityAttribute(json);
        assertArrayEquals(negative, result, 0.0001f);
    }

    @Test
    void shouldHandleLargeVectors() {
        float[] large = new float[1536];
        for (int i = 0; i < 1536; i++) {
            large[i] = (float) (i * 0.001);
        }
        
        String json = converter.convertToDatabaseColumn(large);
        float[] result = converter.convertToEntityAttribute(json);
        
        assertEquals(1536, result.length);
        assertEquals(0.0f, result[0], 0.0001f);
        assertEquals(1.535f, result[1535], 0.0001f);
    }
}
