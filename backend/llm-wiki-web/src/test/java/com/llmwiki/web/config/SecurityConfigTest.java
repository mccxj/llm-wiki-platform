package com.llmwiki.web.config;

import com.llmwiki.web.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtAuthenticationFilter jwtFilter;

    @Test
    void passwordEncoder_shouldReturnBCrypt() {
        SecurityConfig config = new SecurityConfig(jwtFilter);
        PasswordEncoder encoder = config.passwordEncoder();
        assertNotNull(encoder);
        assertTrue(encoder instanceof org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder);
    }

    @Test
    void passwordEncoder_shouldEncodeAndMatch() {
        SecurityConfig config = new SecurityConfig(jwtFilter);
        PasswordEncoder encoder = config.passwordEncoder();

        String rawPassword = "test-password";
        String encoded = encoder.encode(rawPassword);

        assertNotEquals(rawPassword, encoded);
        assertTrue(encoder.matches(rawPassword, encoded));
    }

    @Test
    void passwordEncoder_shouldNotMatchWrongPassword() {
        SecurityConfig config = new SecurityConfig(jwtFilter);
        PasswordEncoder encoder = config.passwordEncoder();

        String encoded = encoder.encode("correct-password");
        assertFalse(encoder.matches("wrong-password", encoded));
    }
}
