package com.llmwiki.web.controller;

import com.llmwiki.web.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    JwtTokenProvider tokenProvider;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    AuthController controller;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode("admin_password")).thenReturn("encoded_admin");
        when(passwordEncoder.encode("user_password")).thenReturn("encoded_user");
        controller.init();
    }

    @Test
    void loginWithCorrectPasswordSucceeds() {
        when(passwordEncoder.matches("password123", "encoded_admin")).thenReturn(true);
        when(tokenProvider.createToken("admin", "ADMIN")).thenReturn("fake-jwt-token");

        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setUsername("admin");
        request.setPassword("password123");

        var response = controller.login(request);

        assertEquals(200, response.getStatusCodeValue());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("fake-jwt-token", body.get("token"));
        assertEquals("admin", body.get("username"));
        assertEquals("ADMIN", body.get("role"));
    }

    @Test
    void loginWithWrongPasswordReturns401() {
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrong");

        var response = controller.login(request);

        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void loginWithNonExistentUsernameReturns401() {
        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("password");

        var response = controller.login(request);

        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void registerCreatesUserWithHashedPassword() {
        when(passwordEncoder.encode("newpassword")).thenReturn("encoded_newpassword");
        when(tokenProvider.createToken("newuser", "USER")).thenReturn("fake-jwt-token");

        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setUsername("newuser");
        request.setPassword("newpassword");

        var response = controller.register(request);

        assertEquals(200, response.getStatusCodeValue());
        verify(passwordEncoder).encode("newpassword");
    }
}
