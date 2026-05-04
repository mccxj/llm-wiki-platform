package com.llmwiki.domain.sync.repository;

import com.llmwiki.domain.sync.entity.WikiSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class WikiSourceRepositoryTest {

    @Autowired
    WikiSourceRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveAndFindById() {
        WikiSource source = WikiSource.builder()
                .name("test-wiki")
                .baseUrl("https://wiki.example.com")
                .adapterClass("com.example.TestAdapter")
                .config("{\"key\":\"value\"}")
                .syncCron("0 */6 * * *")
                .enabled(true)
                .build();

        WikiSource saved = repository.save(source);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        WikiSource found = repository.findById(saved.getId()).orElseThrow();
        assertEquals("test-wiki", found.getName());
        assertEquals("https://wiki.example.com", found.getBaseUrl());
        assertEquals("com.example.TestAdapter", found.getAdapterClass());
        assertTrue(found.getEnabled());
    }

    @Test
    void shouldFindByEnabledTrue() {
        repository.save(WikiSource.builder()
                .name("enabled-wiki").adapterClass("adapter").enabled(true).build());
        repository.save(WikiSource.builder()
                .name("enabled-wiki-2").adapterClass("adapter").enabled(true).build());
        repository.save(WikiSource.builder()
                .name("disabled-wiki").adapterClass("adapter").enabled(false).build());

        List<WikiSource> enabled = repository.findByEnabledTrue();
        assertEquals(2, enabled.size());
        assertTrue(enabled.stream().allMatch(WikiSource::getEnabled));
    }

    @Test
    void shouldReturnEmptyListWhenNoEnabledSources() {
        repository.save(WikiSource.builder()
                .name("disabled").adapterClass("adapter").enabled(false).build());

        List<WikiSource> enabled = repository.findByEnabledTrue();
        assertTrue(enabled.isEmpty());
    }

    @Test
    void shouldDefaultEnabledToTrue() {
        WikiSource source = WikiSource.builder()
                .name("default-enabled")
                .adapterClass("adapter")
                .build();

        WikiSource saved = repository.save(source);
        assertTrue(saved.getEnabled());
    }

    @Test
    void shouldDefaultConfigToJsonEmpty() {
        WikiSource source = WikiSource.builder()
                .name("test")
                .adapterClass("adapter")
                .build();

        WikiSource saved = repository.save(source);
        assertEquals("{}", saved.getConfig());
    }

    @Test
    void shouldDefaultSyncCron() {
        WikiSource source = WikiSource.builder()
                .name("test")
                .adapterClass("adapter")
                .build();

        WikiSource saved = repository.save(source);
        assertEquals("0 */6 * * *", saved.getSyncCron());
    }

    @Test
    void shouldUpdateLastSyncAt() {
        WikiSource source = repository.save(WikiSource.builder()
                .name("test").adapterClass("adapter").build());

        source.setLastSyncAt(java.time.Instant.now());
        repository.save(source);

        WikiSource found = repository.findById(source.getId()).orElseThrow();
        assertNotNull(found.getLastSyncAt());
    }

    @Test
    void shouldFindAll() {
        repository.save(WikiSource.builder().name("s1").adapterClass("a").build());
        repository.save(WikiSource.builder().name("s2").adapterClass("a").build());

        List<WikiSource> all = repository.findAll();
        assertEquals(2, all.size());
    }
}
