package com.example.apigateway.config;

import com.example.apigateway.util.JwtTokenValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/",
            "/health",
            "/actuator/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/webjars/**",
            "/auth/register",
            "/auth/login",
            "/auth/refresh",
            "/auth/forgot-password",
            "/auth/reset-password",
            "/auth/health",
            "/auth/actuator/**",
            "/catalog/actuator/**",
            "/orders/actuator/**",
            "/payments/actuator/**",
            "/auth/v3/api-docs/**",
            "/catalog/v3/api-docs/**",
            "/orders/v3/api-docs/**",
            "/payments/v3/api-docs/**"
    };

    private static final String[] PUBLIC_CATALOG_GET_PATHS = {
            "/catalog/items/**",
            "/catalog/categories/**"
    };

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, JwtTokenValidator jwtTokenValidator) {
        AuthenticationWebFilter jwtAuthFilter = jwtAuthenticationWebFilter(jwtTokenValidator);

        return http
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .logout(logout -> logout.disable())
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(exchanges -> exchanges
                    .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .pathMatchers(HttpMethod.GET, PUBLIC_CATALOG_GET_PATHS).permitAll()
                        .pathMatchers(HttpMethod.HEAD, PUBLIC_CATALOG_GET_PATHS).permitAll()
                        .anyExchange().authenticated()
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((exchange, ex) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        })
                )
                .addFilterAt(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    private AuthenticationWebFilter jwtAuthenticationWebFilter(JwtTokenValidator jwtTokenValidator) {
        ReactiveAuthenticationManager authenticationManager = authentication -> {
            String token = String.valueOf(authentication.getCredentials());
            if (!jwtTokenValidator.validateToken(token)) {
                return Mono.error(new BadCredentialsException("Invalid JWT token"));
            }

            String username = jwtTokenValidator.extractUsername(token);
            List<SimpleGrantedAuthority> authorities = jwtTokenValidator.extractRoles(token).stream()
                    .filter(role -> role != null && !role.isBlank())
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            return Mono.just(new UsernamePasswordAuthenticationToken(username, token, authorities));
            };

            AuthenticationWebFilter authFilter = new AuthenticationWebFilter(authenticationManager);

        authFilter.setServerAuthenticationConverter(exchange -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Mono.empty();
            }

            String token = authHeader.substring(7).trim();
            if (token.isEmpty()) {
                return Mono.empty();
            }

            return Mono.just(new UsernamePasswordAuthenticationToken(token, token));
        });

        return authFilter;
    }
}
