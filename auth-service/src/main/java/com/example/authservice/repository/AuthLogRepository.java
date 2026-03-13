package com.example.authservice.repository;

import com.example.authservice.entity.AuthLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthLogRepository extends JpaRepository<AuthLog, String> {
}
