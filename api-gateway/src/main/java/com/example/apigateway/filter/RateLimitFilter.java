package com.example.apigateway.filter;

import com.example.apigateway.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global Rate Limiting Filter
 * Limits requests per IP address to prevent abuse
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    @Autowired
    private RateLimitConfig rateLimitConfig;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String ip = exchange.getRequest().getRemoteAddress() != null 
            ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
            : "unknown";

        Bucket bucket = rateLimitConfig.resolveBucket(ip);

        if (bucket.tryConsume(1)) {
            logger.debug("Request allowed for IP: {}", ip);
            return chain.filter(exchange);
        } else {
            logger.warn("Rate limit exceeded for IP: {}", ip);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("Content-Type", "application/json");
            
            String errorJson = String.format(
                "{\"timestamp\":\"%s\",\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}",
                java.time.LocalDateTime.now().toString()
            );
            
            return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(errorJson.getBytes()))
            );
        }
    }

    @Override
    public int getOrder() {
        return -2; // Execute before JWT authentication filter
    }
}
