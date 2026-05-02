package com.llmwiki.adapter.extraction;

import com.llmwiki.adapter.api.AiApiClient;
import com.llmwiki.adapter.dto.ExtractionResult;
import com.llmwiki.adapter.dto.ExtractionResult.EntityInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MultiPassExtractorTest {

    AiApiClient aiClient;
    MultiPassExtractor extractor;

    @BeforeEach
    void setUp() {
        aiClient = mock(AiApiClient.class);
        extractor = new MultiPassExtractor(aiClient);
    }

    @Test
    void constructor_shouldThrowOnNullClient() {
        assertThrows(NullPointerException.class, () -> new MultiPassExtractor(null));
    }

    @Test
    void extractAll_shouldReturnEmptyForNullContent() {
        List<EntityInfo> result = extractor.extractAll(null, Set.of("TECH"));
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void extractAll_shouldReturnEmptyForBlankContent() {
        List<EntityInfo> result = extractor.extractAll("   ", Set.of("TECH"));
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void extractAll_singlePass_shouldReturnEntities() {
        ExtractionResult pass1 = new ExtractionResult();
        pass1.setEntities(List.of(
                new EntityInfo("Java", "TECH", "Programming language", List.of()),
                new EntityInfo("Python", "TECH", "Scripting language", List.of())));
        pass1.setConcepts(Collections.emptyList());

        when(aiClient.extractEntities(anyString())).thenReturn(pass1);

        List<EntityInfo> result = extractor.extractAll("test content", Set.of("TECH"));

        assertEquals(2, result.size());
        verify(aiClient, times(1)).extractEntities(anyString());
    }

    @Test
    void extractAll_shouldDeduplicateByName() {
        ExtractionResult pass1 = new ExtractionResult();
        pass1.setEntities(List.of(
                new EntityInfo("Java", "TECH", "Lang", List.of()),
                new EntityInfo("Python", "TECH", "Lang", List.of())));
        pass1.setConcepts(Collections.emptyList());

        ExtractionResult pass2 = new ExtractionResult();
        pass2.setEntities(List.of(
                new EntityInfo("Java", "TECH", "Lang", List.of()),
                new EntityInfo("Rust", "TOOL", "Lang", List.of())));
        pass2.setConcepts(Collections.emptyList());

        when(aiClient.extractEntities(anyString())).thenReturn(pass1).thenReturn(pass2);

        // Request TECH and TOOL types; pass1 only finds TECH, so pass2 targets TOOL
        List<EntityInfo> result = extractor.extractAll("test content", Set.of("TECH", "TOOL"));

        // Should have 3 unique entities: Java, Python, Rust
        assertEquals(3, result.size());
        verify(aiClient, times(2)).extractEntities(anyString());
    }

    @Test
    void extractAll_shouldRunSecondPassForMissedTypes() {
        // Pass 1: only finds TECH entities
        ExtractionResult pass1 = new ExtractionResult();
        pass1.setEntities(List.of(new EntityInfo("Java", "TECH", "Lang", List.of())));
        pass1.setConcepts(Collections.emptyList());

        // Pass 2: finds the missed ORG entity
        ExtractionResult pass2 = new ExtractionResult();
        pass2.setEntities(List.of(new EntityInfo("Google", "ORG", "Company", List.of())));
        pass2.setConcepts(Collections.emptyList());

        when(aiClient.extractEntities(anyString())).thenReturn(pass1).thenReturn(pass2);

        Set<String> allTypes = Set.of("TECH", "ORG");
        List<EntityInfo> result = extractor.extractAll("test content", allTypes);

        // Should have both Java (TECH) and Google (ORG)
        assertEquals(2, result.size());
        verify(aiClient, times(2)).extractEntities(anyString());
    }

    @Test
    void extractAll_shouldNotRunSecondPassWhenAllTypesFound() {
        ExtractionResult pass1 = new ExtractionResult();
        pass1.setEntities(List.of(
                new EntityInfo("Java", "TECH", "Lang", List.of()),
                new EntityInfo("Google", "ORG", "Company", List.of())));
        pass1.setConcepts(Collections.emptyList());

        when(aiClient.extractEntities(anyString())).thenReturn(pass1);

        Set<String> allTypes = Set.of("TECH", "ORG");
        List<EntityInfo> result = extractor.extractAll("test content", allTypes);

        assertEquals(2, result.size());
        // Only 1 call since all types found in pass 1
        verify(aiClient, times(1)).extractEntities(anyString());
    }

    @Test
    void extractAll_shouldHandleNullEntityTypes() {
        ExtractionResult pass1 = new ExtractionResult();
        pass1.setEntities(List.of(new EntityInfo("Java", "TECH", "Lang", List.of())));
        pass1.setConcepts(Collections.emptyList());

        when(aiClient.extractEntities(anyString())).thenReturn(pass1);

        List<EntityInfo> result = extractor.extractAll("test content", null);

        assertEquals(1, result.size());
        verify(aiClient, times(1)).extractEntities(anyString());
    }

    @Test
    void extractAll_shouldHandleEmptyEntityTypes() {
        ExtractionResult pass1 = new ExtractionResult();
        pass1.setEntities(List.of(new EntityInfo("Java", "TECH", "Lang", List.of())));
        pass1.setConcepts(Collections.emptyList());

        when(aiClient.extractEntities(anyString())).thenReturn(pass1);

        List<EntityInfo> result = extractor.extractAll("test content", Collections.emptySet());

        assertEquals(1, result.size());
        verify(aiClient, times(1)).extractEntities(anyString());
    }

    @Test
    void extractAll_shouldHandleNullEntitiesFromAiClient() {
        ExtractionResult pass1 = new ExtractionResult();
        pass1.setEntities(null);
        pass1.setConcepts(null);

        when(aiClient.extractEntities(anyString())).thenReturn(pass1);

        List<EntityInfo> result = extractor.extractAll("test content", Set.of("TECH"));

        assertTrue(result.isEmpty());
    }

    @Test
    void extractAll_shouldDeduplicateCaseInsensitively() {
        ExtractionResult pass1 = new ExtractionResult();
        pass1.setEntities(List.of(new EntityInfo("Java", "TECH", "Lang", List.of())));
        pass1.setConcepts(Collections.emptyList());

        ExtractionResult pass2 = new ExtractionResult();
        pass2.setEntities(List.of(new EntityInfo("java", "TECH", "Lang", List.of())));
        pass2.setConcepts(Collections.emptyList());

        when(aiClient.extractEntities(anyString())).thenReturn(pass1).thenReturn(pass2);

        List<EntityInfo> result = extractor.extractAll("test content", Set.of("TECH"));

        // "Java" and "java" should be deduplicated to 1
        assertEquals(1, result.size());
    }
}
