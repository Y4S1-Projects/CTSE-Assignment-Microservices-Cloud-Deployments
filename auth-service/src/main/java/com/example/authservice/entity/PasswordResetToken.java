package com.example.authservice.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;

    @Column(unique = true, nullable = false)
    private String token;

    private LocalDateTime expiryDate;

    private boolean used;

    @CreatedDate
    private LocalDateTime createdAt;
}
