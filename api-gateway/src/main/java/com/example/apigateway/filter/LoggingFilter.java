package com.example.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Logging Filter for API Gateway
 * Logs all incoming requests and responses
 */
@Component
public class LoggingFilter implements GlobalFilter, Ordered {
    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        
        String method = exchange.getRequest().getMethod().toString();
        String path = exchange.getRequest().getPath().value();
        String ip = exchange.getRequest().getRemoteAddress() != null 
            ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
            : "unknown";

        logger.info("→ Incoming request: {} {} from {}", method, path, ip);

        return chain.filter(exchange).then(
            Mono.fromRunnable(() -> {
                long duration = System.currentTimeMillis() - startTime;
                int statusCode = exchange.getResponse().getStatusCode() != null 
                    ? exchange.getResponse().getStatusCode().value() 
                    : 0;
                    
                logger.info("← Response: {} {} - Status: {} - Duration: {}ms", 
                    method, path, statusCode, duration);
            })
        );
    }

    @Override
    public int getOrder() {
        return -3; // Execute first
    }
}
