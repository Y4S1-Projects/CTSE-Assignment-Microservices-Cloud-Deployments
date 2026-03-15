package com.example.authservice.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;

    @Column(unique = true, nullable = false)
    private String token;

    private LocalDateTime expiryDate;

    private boolean revoked;

    @CreatedDate
    private LocalDateTime createdAt;
}
