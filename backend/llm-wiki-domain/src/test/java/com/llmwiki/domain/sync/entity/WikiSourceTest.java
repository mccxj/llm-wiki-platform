package com.llmwiki.domain.sync.entity;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class WikiSourceTest {

    @Test
    void shouldCreateWithBuilder() {
        WikiSource source = WikiSource.builder()
                .id(UUID.randomUUID())
                .name("Test Wiki")
                .baseUrl("https://wiki.example.com")
                .adapterClass("MockWikiAdapter")
                .config("{\"token\":\"abc\"}")
                .syncCron("0 */6 * * *")
                .enabled(true)
                .build();

        assertNotNull(source.getId());
        assertEquals("Test Wiki", source.getName());
        assertEquals("https://wiki.example.com", source.getBaseUrl());
        assertEquals("MockWikiAdapter", source.getAdapterClass());
        assertEquals("{\"token\":\"abc\"}", source.getConfig());
        assertEquals("0 */6 * * *", source.getSyncCron());
        assertTrue(source.getEnabled());
    }

    @Test
    void shouldHaveDefaultConfig() {
        WikiSource source = WikiSource.builder()
                .name("Test")
                .adapterClass("MockAdapter")
                .build();

        assertEquals("{}", source.getConfig());
        assertEquals("0 */6 * * *", source.getSyncCron());
        assertTrue(source.getEnabled());
    }

    @Test
    void shouldSetLastSyncAt() {
        WikiSource source = WikiSource.builder()
                .name("Test")
                .adapterClass("MockAdapter")
                .build();

        Instant now = Instant.now();
        source.setLastSyncAt(now);
        assertEquals(now, source.getLastSyncAt());
    }

    @Test
    void shouldSupportNoArgsConstructor() {
        WikiSource source = new WikiSource();
        source.setName("Manual");
        source.setBaseUrl("https://example.com");
        source.setAdapterClass("ManualAdapter");

        assertEquals("Manual", source.getName());
        assertEquals("https://example.com", source.getBaseUrl());
        assertEquals("ManualAdapter", source.getAdapterClass());
    }
}
