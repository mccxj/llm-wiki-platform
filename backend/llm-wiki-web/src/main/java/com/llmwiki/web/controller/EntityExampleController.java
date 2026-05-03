package com.llmwiki.web.controller;

import com.llmwiki.domain.example.entity.EntityExample;
import com.llmwiki.service.example.EntityExampleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/examples")
@RequiredArgsConstructor
public class EntityExampleController {

    private final EntityExampleService exampleService;

    public record CreateExampleRequest(String name, String entityType, String exampleText, String extractionData) {}
    public record UpdateExampleRequest(String name, String entityType, String exampleText, String extractionData) {}

    @GetMapping
    public ResponseEntity<List<EntityExample>> list() {
        return ResponseEntity.ok(exampleService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityExample> getById(@PathVariable UUID id) {
        try {
            EntityExample example = exampleService.findById(id);
            return ResponseEntity.ok(example);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/type/{entityType}")
    public ResponseEntity<List<EntityExample>> listByType(@PathVariable String entityType) {
        return ResponseEntity.ok(exampleService.findByType(entityType));
    }

    @PostMapping
    public ResponseEntity<EntityExample> create(@RequestBody CreateExampleRequest request) {
        EntityExample created = exampleService.createExample(
                request.name(),
                request.entityType(),
                request.exampleText(),
                request.extractionData());
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EntityExample> update(@PathVariable UUID id, @RequestBody UpdateExampleRequest request) {
        try {
            EntityExample updated = exampleService.updateExample(
                    id,
                    request.name(),
                    request.entityType(),
                    request.exampleText(),
                    request.extractionData());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        try {
            exampleService.deleteExample(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
