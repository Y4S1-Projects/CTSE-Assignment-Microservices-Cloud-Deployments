package com.example.authservice.service;

import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.LoginResponse;
import com.example.authservice.dto.RegisterRequest;

/**
 * AuthService interface for authentication operations
 */
public interface AuthService {
    LoginResponse login(LoginRequest request);
    LoginResponse register(RegisterRequest request);
    boolean validateToken(String token);
    String extractUserId(String token);
    String extractUsername(String token);
}
