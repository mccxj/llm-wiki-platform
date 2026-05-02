package com.llmwiki.service.example;

import com.llmwiki.adapter.dto.ExampleData;
import com.llmwiki.domain.example.entity.EntityExample;
import com.llmwiki.domain.example.repository.EntityExampleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntityExampleServiceTest {

    @Mock
    EntityExampleRepository repository;

    @InjectMocks
    EntityExampleService service;

    EntityExample sampleExample;

    @BeforeEach
    void setUp() {
        sampleExample = EntityExample.builder()
                .id(UUID.randomUUID())
                .name("Java Example")
                .description("Example for Java")
                .exampleText("Java is a programming language.")
                .extractionData("[{\"extractionClass\":\"TECH\",\"extractionText\":\"Java\",\"description\":\"Programming language\",\"attributes\":[\"object-oriented\"]}]")
                .entityType("ENTITY")
                .build();
    }

    @Test
    void createExample_shouldSaveAndReturn() {
        when(repository.save(any(EntityExample.class))).thenAnswer(i -> {
            EntityExample e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        EntityExample result = service.createExample("Java Example", "ENTITY",
                "Java is a programming language.", "[]");

        assertNotNull(result);
        assertEquals("Java Example", result.getName());
        assertEquals("ENTITY", result.getEntityType());
        assertFalse(result.getDeleted());

        ArgumentCaptor<EntityExample> captor = ArgumentCaptor.forClass(EntityExample.class);
        verify(repository).save(captor.capture());
        assertEquals("Java Example", captor.getValue().getName());
    }

    @Test
    void updateExample_shouldUpdateFields() {
        UUID id = sampleExample.getId();
        when(repository.findById(id)).thenReturn(Optional.of(sampleExample));
        when(repository.save(any(EntityExample.class))).thenAnswer(i -> i.getArgument(0));

        EntityExample result = service.updateExample(id, "Updated Name", "CONCEPT",
                "New text.", "[]");

        assertEquals("Updated Name", result.getName());
        assertEquals("CONCEPT", result.getEntityType());
        assertEquals("New text.", result.getExampleText());
        verify(repository).save(sampleExample);
    }

    @Test
    void updateExample_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.updateExample(id, "Name", "ENTITY", "text", "[]"));
    }

    @Test
    void deleteExample_shouldSoftDelete() {
        UUID id = sampleExample.getId();
        when(repository.findById(id)).thenReturn(Optional.of(sampleExample));
        when(repository.save(any(EntityExample.class))).thenAnswer(i -> i.getArgument(0));

        service.deleteExample(id);

        verify(repository).save(sampleExample);
        assertTrue(sampleExample.getDeleted());
    }

    @Test
    void deleteExample_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.deleteExample(id));
    }

    @Test
    void findById_shouldReturnExample() {
        UUID id = sampleExample.getId();
        when(repository.findById(id)).thenReturn(Optional.of(sampleExample));

        EntityExample result = service.findById(id);
        assertNotNull(result);
        assertEquals("Java Example", result.getName());
    }

    @Test
    void findById_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.findById(id));
    }

    @Test
    void findAll_shouldReturnAllActive() {
        List<EntityExample> examples = List.of(sampleExample);
        when(repository.findAllActive()).thenReturn(examples);

        List<EntityExample> result = service.findAll();
        assertEquals(1, result.size());
        assertEquals("Java Example", result.get(0).getName());
    }

    @Test
    void findByType_shouldReturnMatchingType() {
        when(repository.findByEntityTypeAndDeletedFalse("ENTITY"))
                .thenReturn(List.of(sampleExample));

        List<EntityExample> result = service.findByType("ENTITY");
        assertEquals(1, result.size());
        assertEquals("ENTITY", result.get(0).getEntityType());
    }

    @Test
    void loadExamplesAsExampleData_shouldConvertAll() {
        EntityExample example1 = EntityExample.builder()
                .id(UUID.randomUUID())
                .name("Ex1")
                .description("Example 1")
                .exampleText("Java is a language.")
                .extractionData("[{\"extractionClass\":\"TECH\",\"extractionText\":\"Java\",\"description\":\"Programming language\",\"attributes\":[\"OOP\"]}]")
                .entityType("ENTITY")
                .build();
        EntityExample example2 = EntityExample.builder()
                .id(UUID.randomUUID())
                .name("Ex2")
                .description("Example 2")
                .exampleText("Python is a language.")
                .extractionData("[{\"extractionClass\":\"TECH\",\"extractionText\":\"Python\",\"description\":\"Scripting language\",\"attributes\":[\"dynamic\"]}]")
                .entityType("ENTITY")
                .build();

        when(repository.findByEntityTypeAndDeletedFalse("ENTITY"))
                .thenReturn(List.of(example1, example2));

        List<ExampleData> result = service.loadExamplesAsExampleData("ENTITY");

        assertEquals(2, result.size());
        assertEquals("Java is a language.", result.get(0).getText());
        assertEquals(1, result.get(0).getExtractions().size());
        assertEquals("TECH", result.get(0).getExtractions().get(0).getExtractionClass());
        assertEquals("Java", result.get(0).getExtractions().get(0).getExtractionText());
        assertEquals("Programming language", result.get(0).getExtractions().get(0).getDescription());
        assertEquals(List.of("OOP"), result.get(0).getExtractions().get(0).getAttributes());

        assertEquals("Python is a language.", result.get(1).getText());
    }

    @Test
    void loadExamplesAsExampleData_shouldHandleEmptyList() {
        when(repository.findByEntityTypeAndDeletedFalse("ENTITY"))
                .thenReturn(List.of());

        List<ExampleData> result = service.loadExamplesAsExampleData("ENTITY");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void loadExamplesAsExampleData_shouldHandleNullExtractions() {
        EntityExample example = EntityExample.builder()
                .id(UUID.randomUUID())
                .name("Ex1")
                .description("Example")
                .exampleText("Text.")
                .extractionData(null)
                .entityType("ENTITY")
                .build();

        when(repository.findByEntityTypeAndDeletedFalse("ENTITY"))
                .thenReturn(List.of(example));

        List<ExampleData> result = service.loadExamplesAsExampleData("ENTITY");
        assertEquals(1, result.size());
        assertEquals("Text.", result.get(0).getText());
        assertTrue(result.get(0).getExtractions().isEmpty());
    }

    @Test
    void loadExamplesAsExampleData_shouldHandleEmptyExtractions() {
        EntityExample example = EntityExample.builder()
                .id(UUID.randomUUID())
                .name("Ex1")
                .description("Example")
                .exampleText("Text.")
                .extractionData("[]")
                .entityType("ENTITY")
                .build();

        when(repository.findByEntityTypeAndDeletedFalse("ENTITY"))
                .thenReturn(List.of(example));

        List<ExampleData> result = service.loadExamplesAsExampleData("ENTITY");
        assertEquals(1, result.size());
        assertTrue(result.get(0).getExtractions().isEmpty());
    }
}
