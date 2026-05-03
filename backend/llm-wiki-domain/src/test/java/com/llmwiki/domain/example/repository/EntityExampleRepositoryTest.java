package com.llmwiki.domain.example.repository;

import com.llmwiki.domain.example.entity.EntityExample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class EntityExampleRepositoryTest {

    @Autowired
    EntityExampleRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveAndFindById() {
        EntityExample example = EntityExample.builder()
                .name("Test Example")
                .entityType("ENTITY")
                .exampleText("Sample text.")
                .extractionData("[{\"extractionClass\":\"TECH\",\"extractionText\":\"Java\"}]")
                .build();

        EntityExample saved = repository.save(example);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        EntityExample found = repository.findById(saved.getId()).orElseThrow();
        assertEquals("Test Example", found.getName());
        assertEquals("ENTITY", found.getEntityType());
        assertEquals("Sample text.", found.getExampleText());
    }

    @Test
    void shouldFindByType() {
        repository.save(EntityExample.builder()
                .name("Entity Example").entityType("ENTITY")
                .exampleText("Text.").extractionData("[]").build());
        repository.save(EntityExample.builder()
                .name("Another Entity").entityType("ENTITY")
                .exampleText("Text 2.").extractionData("[]").build());
        repository.save(EntityExample.builder()
                .name("Concept Example").entityType("CONCEPT")
                .exampleText("Text 3.").extractionData("[]").build());

        List<EntityExample> entities = repository.findByEntityType("ENTITY");
        assertEquals(2, entities.size());
        assertTrue(entities.stream().allMatch(e -> "ENTITY".equals(e.getEntityType())));

        List<EntityExample> concepts = repository.findByEntityType("CONCEPT");
        assertEquals(1, concepts.size());
    }

    @Test
    void shouldFindAllActive() {
        repository.save(EntityExample.builder()
                .name("Active").entityType("ENTITY")
                .exampleText("Text.").extractionData("[]").build());
        repository.save(EntityExample.builder()
                .name("Active 2").entityType("ENTITY")
                .exampleText("Text 2.").extractionData("[]").build());
        EntityExample deleted = EntityExample.builder()
                .name("Deleted").entityType("ENTITY")
                .exampleText("Text 3.").extractionData("[]").deleted(true).build();
        repository.save(deleted);

        List<EntityExample> active = repository.findAllActive();
        assertEquals(2, active.size());
        assertTrue(active.stream().noneMatch(EntityExample::getDeleted));
    }

    @Test
    void shouldFindByTypeAndActive() {
        repository.save(EntityExample.builder()
                .name("Active Entity").entityType("ENTITY")
                .exampleText("Text.").extractionData("[]").build());
        repository.save(EntityExample.builder()
                .name("Deleted Entity").entityType("ENTITY")
                .exampleText("Text 2.").extractionData("[]").deleted(true).build());
        repository.save(EntityExample.builder()
                .name("Active Concept").entityType("CONCEPT")
                .exampleText("Text 3.").extractionData("[]").build());

        List<EntityExample> activeEntities = repository.findByEntityTypeAndDeletedFalse("ENTITY");
        assertEquals(1, activeEntities.size());
        assertEquals("Active Entity", activeEntities.get(0).getName());
    }

    @Test
    void shouldFindAll() {
        repository.save(EntityExample.builder()
                .name("Ex1").entityType("ENTITY").exampleText("T1").extractionData("[]").build());
        repository.save(EntityExample.builder()
                .name("Ex2").entityType("CONCEPT").exampleText("T2").extractionData("[]").build());

        List<EntityExample> all = repository.findAll();
        assertEquals(2, all.size());
    }
}
