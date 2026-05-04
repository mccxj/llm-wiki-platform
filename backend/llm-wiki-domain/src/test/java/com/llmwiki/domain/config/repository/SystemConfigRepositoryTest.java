package com.llmwiki.domain.config.repository;

import com.llmwiki.domain.config.entity.SystemConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SystemConfigRepositoryTest {

    @Autowired
    SystemConfigRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveAndFindByKey() {
        SystemConfig config = SystemConfig.builder()
                .key("scoring.threshold")
                .value("6.0")
                .description("Minimum score threshold")
                .build();

        SystemConfig saved = repository.save(config);
        assertNotNull(saved.getUpdatedAt());

        Optional<SystemConfig> found = repository.findByKey("scoring.threshold");
        assertTrue(found.isPresent());
        assertEquals("6.0", found.get().getValue());
        assertEquals("Minimum score threshold", found.get().getDescription());
    }

    @Test
    void shouldReturnEmptyForUnknownKey() {
        Optional<SystemConfig> found = repository.findByKey("nonexistent.key");
        assertFalse(found.isPresent());
    }

    @Test
    void shouldUpdateValue() {
        repository.save(SystemConfig.builder()
                .key("test.key").value("old-value").build());

        SystemConfig config = repository.findByKey("test.key").orElseThrow();
        config.setValue("new-value");
        repository.save(config);

        SystemConfig found = repository.findByKey("test.key").orElseThrow();
        assertEquals("new-value", found.getValue());
    }

    @Test
    void shouldUseStringKey() {
        SystemConfig config = SystemConfig.builder()
                .key("my.config.key")
                .value("my-value")
                .build();

        SystemConfig saved = repository.save(config);
        assertEquals("my.config.key", saved.getKey());

        SystemConfig found = repository.findById("my.config.key").orElseThrow();
        assertEquals("my-value", found.getValue());
    }

    @Test
    void shouldFindAll() {
        repository.save(SystemConfig.builder().key("key1").value("v1").build());
        repository.save(SystemConfig.builder().key("key2").value("v2").build());
        repository.save(SystemConfig.builder().key("key3").value("v3").build());

        List<SystemConfig> all = repository.findAll();
        assertEquals(3, all.size());
    }

    @Test
    void shouldDeleteByKey() {
        repository.save(SystemConfig.builder().key("to-delete").value("val").build());
        assertTrue(repository.findByKey("to-delete").isPresent());

        repository.deleteById("to-delete");
        assertFalse(repository.findByKey("to-delete").isPresent());
    }

    @Test
    void shouldHandleNullDescription() {
        SystemConfig config = SystemConfig.builder()
                .key("no-desc")
                .value("value")
                .build();

        SystemConfig saved = repository.save(config);
        assertNull(saved.getDescription());
    }

    @Test
    void shouldSetUpdatedAtOnPersist() {
        SystemConfig config = SystemConfig.builder()
                .key("test")
                .value("val")
                .build();

        SystemConfig saved = repository.save(config);
        assertNotNull(saved.getUpdatedAt());
    }
}
