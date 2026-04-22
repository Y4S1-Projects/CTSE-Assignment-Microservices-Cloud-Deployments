package com.example.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.example.authservice.entity.Role;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login/Registration response containing JWT token and user details")
public class LoginResponse {
    
    @Schema(description = "JWT authentication token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "Access token (short-lived)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "Refresh token (long-lived)", example = "2f2d45d5-cde7-4c26-b6a4-cf7f39f2ad03")
    private String refreshToken;
    
    @Schema(description = "User email", example = "john.doe@example.com")
    private String email;
    
    @Schema(description = "User unique identifier (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String userId;

    @Schema(description = "User role", example = "CUSTOMER")
    private Role role;
}
