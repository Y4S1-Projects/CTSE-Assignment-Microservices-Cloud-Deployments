package com.example.authservice.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;

    private String action;

    private String ipAddress;

    @CreatedDate
    private LocalDateTime timestamp;
}
