package com.llmwiki.web.controller;

import com.llmwiki.web.security.JwtTokenProvider;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证控制器
 * 简化版：内存用户存储，实际项目应使用数据库存储用户
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    // 内存用户存储（仅用于开发/演示）
    private Map<String, String> USERS = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        USERS.put("admin", passwordEncoder.encode("admin_password"));
        USERS.put("user", passwordEncoder.encode("user_password"));
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String storedHash = USERS.get(request.getUsername());
        if (storedHash == null) {
            return ResponseEntity.status(401).body(Map.of("error", "用户不存在"));
        }

        if (!passwordEncoder.matches(request.getPassword(), storedHash)) {
            return ResponseEntity.status(401).body(Map.of("error", "密码错误"));
        }

        String role = request.getUsername().equals("admin") ? "ADMIN" : "USER";
        String token = tokenProvider.createToken(request.getUsername(), role);
        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", request.getUsername(),
                "role", role
        ));
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequest request) {
        if (USERS.containsKey(request.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名已存在"));
        }
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        USERS.put(request.getUsername(), encodedPassword);
        String token = tokenProvider.createToken(request.getUsername(), "USER");
        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", request.getUsername(),
                "role", "USER"
        ));
    }

    /**
     * 验证Token
     */
    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of("error", "缺少Token"));
        }
        String token = authHeader.substring(7);
        boolean valid = tokenProvider.validateToken(token);
        if (valid) {
            String userId = tokenProvider.getUserId(token);
            String role = tokenProvider.getRole(token);
            return ResponseEntity.ok(Map.of("valid", true, "userId", userId, "role", role));
        }
        return ResponseEntity.ok(Map.of("valid", false));
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}
