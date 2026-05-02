package com.llmwiki.web.controller;

import com.llmwiki.domain.example.entity.EntityExample;
import com.llmwiki.service.example.EntityExampleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntityExampleControllerTest {

    @Mock
    EntityExampleService exampleService;

    @InjectMocks
    EntityExampleController controller;

    @Test
    void list_shouldReturnAllExamples() {
        List<EntityExample> examples = List.of(
                EntityExample.builder().id(UUID.randomUUID()).name("Ex1").entityType("ENTITY").build(),
                EntityExample.builder().id(UUID.randomUUID()).name("Ex2").entityType("ENTITY").build());
        when(exampleService.findAll()).thenReturn(examples);

        var response = controller.list();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void listByType_shouldReturnFilteredExamples() {
        List<EntityExample> examples = List.of(
                EntityExample.builder().id(UUID.randomUUID()).name("Ex1").entityType("ENTITY").build());
        when(exampleService.findByType("ENTITY")).thenReturn(examples);

        var response = controller.listByType("ENTITY");

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void create_shouldReturnCreatedExample() {
        EntityExample created = EntityExample.builder()
                .id(UUID.randomUUID()).name("New Example").entityType("ENTITY")
                .exampleText("Text.").extractionData("[]").build();
        when(exampleService.createExample(eq("New Example"), eq("ENTITY"), eq("Text."), eq("[]")))
                .thenReturn(created);

        var response = controller.create(new EntityExampleController.CreateExampleRequest(
                "New Example", "ENTITY", "Text.", "[]"));

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("New Example", response.getBody().getName());
    }

    @Test
    void update_shouldReturnUpdatedExample() {
        UUID id = UUID.randomUUID();
        EntityExample updated = EntityExample.builder()
                .id(id).name("Updated").entityType("CONCEPT")
                .exampleText("New text.").extractionData("[]").build();
        when(exampleService.updateExample(eq(id), eq("Updated"), eq("CONCEPT"), eq("New text."), eq("[]")))
                .thenReturn(updated);

        var response = controller.update(id, new EntityExampleController.UpdateExampleRequest(
                "Updated", "CONCEPT", "New text.", "[]"));

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Updated", response.getBody().getName());
        assertEquals("CONCEPT", response.getBody().getEntityType());
    }

    @Test
    void update_shouldReturn404WhenNotFound() {
        UUID id = UUID.randomUUID();
        when(exampleService.updateExample(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("EntityExample not found: " + id));

        var response = controller.update(id, new EntityExampleController.UpdateExampleRequest(
                "Name", "ENTITY", "text", "[]"));

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void delete_shouldReturnOk() {
        UUID id = UUID.randomUUID();
        doNothing().when(exampleService).deleteExample(id);

        var response = controller.delete(id);

        assertEquals(200, response.getStatusCodeValue());
        verify(exampleService).deleteExample(id);
    }

    @Test
    void delete_shouldReturn404WhenNotFound() {
        UUID id = UUID.randomUUID();
        doThrow(new IllegalArgumentException("EntityExample not found: " + id))
                .when(exampleService).deleteExample(id);

        var response = controller.delete(id);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void getById_shouldReturnExample() {
        UUID id = UUID.randomUUID();
        EntityExample example = EntityExample.builder()
                .id(id).name("Test").entityType("ENTITY").build();
        when(exampleService.findById(id)).thenReturn(example);

        var response = controller.getById(id);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Test", response.getBody().getName());
    }

    @Test
    void getById_shouldReturn404WhenNotFound() {
        UUID id = UUID.randomUUID();
        when(exampleService.findById(id))
                .thenThrow(new IllegalArgumentException("EntityExample not found: " + id));

        var response = controller.getById(id);

        assertEquals(404, response.getStatusCodeValue());
    }
}
