package com.example.authservice.dto;

import com.example.authservice.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateUserRequest {
    private String email;
    private String fullName;
    private Role role;
    private Boolean active;
}
