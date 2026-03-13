package com.example.authservice.service;

import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.LoginResponse;
import com.example.authservice.dto.RefreshRequest;
import com.example.authservice.dto.RegisterRequest;
import com.example.authservice.dto.ChangePasswordRequest;
import com.example.authservice.dto.ForgotPasswordRequest;
import com.example.authservice.dto.ResetPasswordRequest;
import com.example.authservice.entity.User;

/**
 * AuthService interface for authentication operations
 */
public interface AuthService {
    LoginResponse login(LoginRequest request);
    LoginResponse register(RegisterRequest request);
    LoginResponse refresh(RefreshRequest request);
    void logout(RefreshRequest request);
    void changePassword(String username, ChangePasswordRequest request);
    String forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    boolean validateToken(String token);
    String extractUserId(String token);
    String extractUsername(String token);
    String extractRole(String token);
    User getUserByUsername(String username);
}
