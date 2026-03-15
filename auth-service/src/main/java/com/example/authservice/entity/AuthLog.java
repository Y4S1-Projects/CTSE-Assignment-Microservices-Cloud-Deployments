package com.example.authservice.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "auth_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthLog {

    @Id
    private String id;

    private String userId;

    private String action;

    private String ipAddress;

    @CreatedDate
    private LocalDateTime timestamp;
}
