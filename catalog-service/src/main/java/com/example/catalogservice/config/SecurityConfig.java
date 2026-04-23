package com.example.catalogservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for catalog-service.
 * JWT authentication is handled by the API Gateway — this service
 * trusts all inbound traffic that has already passed the gateway filter.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, GatewayAuthenticationFilter gatewayAuthenticationFilter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(HttpBasicConfigurer::disable)
            .formLogin(FormLoginConfigurer::disable)
            .logout(LogoutConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(gatewayAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/actuator/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/items/**", "/categories").permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/dashboard").hasRole("ADMIN")
                    .requestMatchers(org.springframework.http.HttpMethod.POST, "/items/*/decrement-stock").hasAnyRole("ADMIN", "SERVICE_PAYMENT")
                    .requestMatchers(org.springframework.http.HttpMethod.POST, "/items").hasRole("ADMIN")
                    .requestMatchers(org.springframework.http.HttpMethod.PUT, "/items/*").hasRole("ADMIN")
                    .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/items/*/stock").hasRole("ADMIN")
                    .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/items/*").hasRole("ADMIN")
                    .anyRequest().denyAll());

        return http.build();
    }
}
