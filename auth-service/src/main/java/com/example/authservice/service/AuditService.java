package com.example.authservice.service;

import com.example.authservice.entity.AuthLog;
import com.example.authservice.repository.AuthLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    @Autowired
    private AuthLogRepository authLogRepository;

    public void log(String userId, String action, String ipAddress) {
        authLogRepository.save(AuthLog.builder()
                .userId(userId)
                .action(action)
                .ipAddress(ipAddress == null ? "unknown" : ipAddress)
                .build());
    }
}
