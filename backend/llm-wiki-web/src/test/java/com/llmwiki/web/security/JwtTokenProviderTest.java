package com.llmwiki.web.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(
                "llm-wiki-platform-secret-key-at-least-256-bits-long-for-hs256",
                86400000L);
    }

    @Test
    void createToken_shouldReturnNonNullToken() {
        String token = tokenProvider.createToken("user-123", "ADMIN");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void createToken_shouldGenerateValidToken() {
        String token = tokenProvider.createToken("user-123", "ADMIN");
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    void getUserId_shouldReturnCorrectUserId() {
        String token = tokenProvider.createToken("user-456", "USER");
        assertEquals("user-456", tokenProvider.getUserId(token));
    }

    @Test
    void getRole_shouldReturnCorrectRole() {
        String token = tokenProvider.createToken("user-789", "ADMIN");
        assertEquals("ADMIN", tokenProvider.getRole(token));
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        String token = tokenProvider.createToken("user-001", "USER");
        assertTrue(tokenProvider.validateToken(token));
    }

    @Test
    void validateToken_shouldReturnFalseForInvalidToken() {
        assertFalse(tokenProvider.validateToken("invalid.token.value"));
    }

    @Test
    void validateToken_shouldReturnFalseForEmptyToken() {
        assertFalse(tokenProvider.validateToken(""));
    }

    @Test
    void validateToken_shouldReturnFalseForTamperedToken() {
        String token = tokenProvider.createToken("user-001", "USER");
        String tampered = token.substring(0, token.length() - 2) + "XX";
        assertFalse(tokenProvider.validateToken(tampered));
    }

    @Test
    void validateToken_shouldReturnFalseForTokenSignedWithDifferentKey() {
        JwtTokenProvider otherProvider = new JwtTokenProvider(
                "different-secret-key-at-least-256-bits-long-for-testing",
                86400000L);
        String token = otherProvider.createToken("user-001", "USER");
        assertFalse(tokenProvider.validateToken(token));
    }
}
