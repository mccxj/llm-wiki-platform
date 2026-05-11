package com.llmwiki.domain.graph.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * JPA AttributeConverter for converting float[] to/from JSON string.
 * MariaDB VECTOR type stores vectors as JSON arrays, so this converter
 * handles the serialization/deserialization of float arrays for JPA entities.
 */
@Converter
public class FloatArrayToJsonConverter implements AttributeConverter<float[], String> {

    private static final String OPEN_BRACKET = "[";
    private static final String CLOSE_BRACKET = "]";
    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        if (attribute == null || attribute.length == 0) {
            return OPEN_BRACKET + CLOSE_BRACKET;
        }
        
        StringBuilder sb = new StringBuilder(OPEN_BRACKET);
        for (int i = 0; i < attribute.length; i++) {
            sb.append(attribute[i]);
            if (i < attribute.length - 1) {
                sb.append(DELIMITER);
            }
        }
        sb.append(CLOSE_BRACKET);
        return sb.toString();
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty() || 
            dbData.equals(OPEN_BRACKET + CLOSE_BRACKET)) {
            return new float[0];
        }
        
        String trimmed = dbData.trim();
        if (!trimmed.startsWith(OPEN_BRACKET) || !trimmed.endsWith(CLOSE_BRACKET)) {
            throw new IllegalArgumentException("Invalid JSON array format: " + dbData);
        }
        
        String content = trimmed.substring(1, trimmed.length() - 1).trim();
        if (content.isEmpty()) {
            return new float[0];
        }
        
        String[] parts = content.split(DELIMITER);
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}
