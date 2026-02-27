package com.example.apigateway.filter;

import com.example.apigateway.util.JwtTokenValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Global JWT Authentication Filter for API Gateway
 * Validates JWT tokens on all requests except public endpoints
 * Adds user information to request headers for downstream services
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtTokenValidator jwtTokenValidator;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/auth/register",
            "/auth/login",
            "/auth/validate",
            "/auth/health",
            "/actuator/",        // All actuator endpoints
            "/auth/actuator/",   // Auth service actuator endpoints via gateway
            "/catalog/actuator/",// Catalog service actuator endpoints via gateway
            "/orders/actuator/", // Order service actuator endpoints via gateway
            "/payments/actuator/", // Payment service actuator endpoints via gateway
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        logger.debug("Processing request: {} {}", request.getMethod(), path);

        // Check if the request path is a public endpoint
        if (isPublicEndpoint(path)) {
            logger.debug("Public endpoint accessed: {}", path);
            return chain.filter(exchange);
        }

        // Extract token from Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header for path: {}", path);
            return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        // Validate JWT token
        if (!jwtTokenValidator.validateToken(token)) {
            logger.warn("Invalid or expired JWT token for path: {}", path);
            return onError(exchange, "Invalid or expired JWT token", HttpStatus.UNAUTHORIZED);
        }

        // Extract user information from token
        String userId = jwtTokenValidator.extractUserId(token);
        String username = jwtTokenValidator.extractUsername(token);
        List<String> roles = jwtTokenValidator.extractRoles(token);

        logger.info("Authenticated user: {} (ID: {}) accessing: {}", username, userId, path);

        // Add user information to request headers for downstream services
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", userId != null ? userId : "")
                .header("X-Username", username != null ? username : "")
                .header("X-User-Roles", String.join(",", roles))
                .build();

        // Continue with the modified request
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * Check if the request path is a public endpoint
     */
    private boolean isPublicEndpoint(String path) {
        for (String endpoint : PUBLIC_ENDPOINTS) {
            if (path.startsWith(endpoint)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handle authentication errors
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");
        
        String errorJson = String.format(
            "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
            java.time.LocalDateTime.now().toString(),
            status.value(),
            status.getReasonPhrase(),
            message,
            exchange.getRequest().getPath().value()
        );
        
        return response.writeWith(
            Mono.just(response.bufferFactory().wrap(errorJson.getBytes()))
        );
    }

    @Override
    public int getOrder() {
        return -1; // High priority - execute before other filters
    }
}
