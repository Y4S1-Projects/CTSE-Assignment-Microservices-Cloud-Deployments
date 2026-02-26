package com.example.authservice.service;

import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.LoginResponse;
import com.example.authservice.dto.RegisterRequest;

/**
 * AuthService interface
 * Implementation will be added during feature development
 */
public interface AuthService {
    LoginResponse login(LoginRequest request);
    LoginResponse register(RegisterRequest request);
    boolean validateToken(String token);
    String getUserIdFromToken(String token);
}
