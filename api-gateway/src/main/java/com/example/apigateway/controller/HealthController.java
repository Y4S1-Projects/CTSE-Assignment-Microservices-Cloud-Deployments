package com.example.apigateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller for API Gateway
 */
@RestController
@RequestMapping
public class HealthController {

    @Value("${spring.application.name:api-gateway}")
    private String applicationName;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", applicationName);
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("port", 8080);
        return ResponseEntity.ok(health);
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", applicationName);
        info.put("version", "1.0.0");
        info.put("description", "API Gateway for Food Ordering Microservices");
        info.put("status", "running");
        info.put("timestamp", LocalDateTime.now().toString());
        
        Map<String, String> routes = new HashMap<>();
        routes.put("Authentication", "/auth/**");
        routes.put("Catalog", "/catalog/**");
        routes.put("Orders", "/orders/**");
        routes.put("Payments", "/payments/**");
        routes.put("Health", "/health");
        routes.put("Actuator", "/actuator/health");
        routes.put("Swagger UI", "/swagger-ui.html");
        routes.put("API Docs", "/v3/api-docs");
        
        info.put("routes", routes);
        return ResponseEntity.ok(info);
    }
}
