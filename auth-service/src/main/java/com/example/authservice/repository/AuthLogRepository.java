package com.example.authservice.repository;

import com.example.authservice.entity.AuthLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuthLogRepository extends MongoRepository<AuthLog, String> {
}
