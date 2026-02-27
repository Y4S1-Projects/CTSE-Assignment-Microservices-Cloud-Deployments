package com.example.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User registration request")
public class RegisterRequest {
    
    @Schema(description = "Desired username (must be unique)", example = "john.doe", required = true)
    private String username;
    
    @Schema(description = "Email address (must be unique)", example = "john.doe@example.com", required = true)
    private String email;
    
    @Schema(description = "Password (minimum 6 characters)", example = "password123", required = true, minLength = 6)
    private String password;
    
    @Schema(description = "Full name of the user", example = "John Doe", required = true)
    private String fullName;
}
