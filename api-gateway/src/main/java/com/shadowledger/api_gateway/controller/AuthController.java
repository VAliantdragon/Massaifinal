package com.shadowledger.api_gateway.controller;

import com.shadowledger.api_gateway.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/token")
    public ResponseEntity<Map<String, String>> generateToken(@RequestBody(required = false) Map<String, String> credentials) {
        // Validate input
        if (credentials == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Bad Request");
            error.put("message", "Request body is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        String username = credentials.get("username");
        if (username == null || username.trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Bad Request");
            error.put("message", "Username is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        String role = credentials.getOrDefault("role", "USER");

        // Validate role
        if (!isValidRole(role)) {
            role = "USER";
        }

        try {
            String token = jwtService.generateToken(username, role);

            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("role", role);
            response.put("username", username);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            error.put("message", "Failed to generate token: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    private boolean isValidRole(String role) {
        return role != null && (role.equals("USER") || role.equals("AUDITOR") || role.equals("ADMIN"));
    }
}
