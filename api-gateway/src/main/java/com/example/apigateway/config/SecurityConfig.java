package com.example.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchanges -> exchanges
                        // Allow public access to actuator endpoints
                        .pathMatchers("/actuator/**").permitAll()
                        // Allow public access to Swagger UI and OpenAPI docs
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                        // Allow public access to all gateway routes (for testing)
                        .pathMatchers("/auth/**", "/catalog/**", "/orders/**", "/payments/**", "/notifications/**").permitAll()
                        // All other requests require authentication
                        .anyExchange().authenticated()
                )
                .build();
    }
}
