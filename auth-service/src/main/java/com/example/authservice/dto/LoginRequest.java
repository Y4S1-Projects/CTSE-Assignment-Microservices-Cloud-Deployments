package com.example.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login request containing user credentials")
public class LoginRequest {
    
    @Schema(description = "User email address", example = "john.doe@example.com", required = true)
    private String email;
    
    @Schema(description = "User password", example = "password123", required = true)
    private String password;
}
