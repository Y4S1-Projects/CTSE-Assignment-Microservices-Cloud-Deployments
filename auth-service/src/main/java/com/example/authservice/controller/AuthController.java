package com.example.authservice.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.LoginResponse;
import com.example.authservice.dto.RegisterRequest;
import com.example.authservice.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * AuthController - REST endpoints for authentication
 */
@RestController
@RequestMapping
@Tag(name = "Authentication", description = "Authentication endpoints for user login and registration")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    @Operation(
        summary = "User login",
        description = "Authenticate user with email and password, returns JWT token"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        logger.info("Login request received for: {}", request.getEmail());
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(
        summary = "User registration",
        description = "Register a new user and return JWT token"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Registration successful",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "409", description = "User already exists"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<LoginResponse> register(@RequestBody RegisterRequest request) {
        logger.info("Registration request received for: {}", request.getEmail());
        LoginResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/validate")
    @Operation(
        summary = "Validate JWT token",
        description = "Validate a JWT token and extract user information"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token is valid"),
        @ApiResponse(responseCode = "401", description = "Token is invalid or expired")
    })
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader("Authorization") String authHeader) {
        logger.debug("Token validation request received");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "message", "Invalid authorization header"));
        }

        String token = authHeader.substring(7);
        boolean isValid = authService.validateToken(token);

        if (isValid) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("userId", authService.extractUserId(token));
            response.put("username", authService.extractUsername(token));
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "message", "Invalid or expired token"));
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the auth service is running")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "auth-service"
        ));
    }
}
