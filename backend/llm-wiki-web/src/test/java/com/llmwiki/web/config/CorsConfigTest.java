package com.llmwiki.web.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {

    @Test
    void corsConfigurationSourceShouldNotBeNull() {
        CorsConfig config = new CorsConfig();
        assertNotNull(config.corsConfigurationSource());
    }

    private CorsConfiguration getCorsConfig() {
        CorsConfig config = new CorsConfig();
        return config.corsConfigurationSource()
                .getCorsConfiguration(new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/test"));
    }

    @Test
    void corsConfigurationShouldAllowConfiguredOrigins() {
        CorsConfiguration corsConfig = getCorsConfig();

        assertNotNull(corsConfig);
        assertTrue(corsConfig.getAllowedOrigins().contains("http://localhost:3000"));
        assertTrue(corsConfig.getAllowedOrigins().contains("http://localhost:5173"));
        assertTrue(corsConfig.getAllowedOrigins().contains("http://localhost"));
    }

    @Test
    void corsConfigurationShouldAllowConfiguredMethods() {
        CorsConfiguration corsConfig = getCorsConfig();

        assertTrue(corsConfig.getAllowedMethods().contains("GET"));
        assertTrue(corsConfig.getAllowedMethods().contains("POST"));
        assertTrue(corsConfig.getAllowedMethods().contains("PUT"));
        assertTrue(corsConfig.getAllowedMethods().contains("DELETE"));
        assertTrue(corsConfig.getAllowedMethods().contains("OPTIONS"));
    }

    @Test
    void corsConfigurationShouldAllowAllHeaders() {
        CorsConfiguration corsConfig = getCorsConfig();

        assertTrue(corsConfig.getAllowedHeaders().contains("*"));
    }

    @Test
    void corsConfigurationShouldAllowCredentials() {
        CorsConfiguration corsConfig = getCorsConfig();

        assertTrue(corsConfig.getAllowCredentials());
    }

    @Test
    void corsConfigurationShouldHaveMaxAge() {
        CorsConfiguration corsConfig = getCorsConfig();

        assertEquals(3600L, corsConfig.getMaxAge());
    }
}
