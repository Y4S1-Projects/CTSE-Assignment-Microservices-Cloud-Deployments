package com.example.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login/Registration response containing JWT token and user details")
public class LoginResponse {
    
    @Schema(description = "JWT authentication token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;
    
    @Schema(description = "Username", example = "john.doe")
    private String username;
    
    @Schema(description = "User unique identifier (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String userId;
}
