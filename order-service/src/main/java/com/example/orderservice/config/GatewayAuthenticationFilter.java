package com.example.orderservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLES_HEADER = "X-User-Roles";
    private static final String SERVICE_ROLE_HEADER = "X-Service-Role";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            Authentication authentication = buildAuthentication(request);
            if (authentication != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private Authentication buildAuthentication(HttpServletRequest request) {
        String serviceRole = request.getHeader(SERVICE_ROLE_HEADER);
        if (serviceRole != null && !serviceRole.isBlank()) {
            List<GrantedAuthority> authorities = new ArrayList<>();
            for (String role : splitRoles(serviceRole)) {
                authorities.add(new SimpleGrantedAuthority(normalizeRole(role)));
            }
            return new UsernamePasswordAuthenticationToken("service-payment", "N/A", authorities);
        }

        String userId = request.getHeader(USER_ID_HEADER);
        if (userId == null || userId.isBlank()) {
            return null;
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        String rolesHeader = request.getHeader(USER_ROLES_HEADER);
        for (String role : splitRoles(rolesHeader)) {
            authorities.add(new SimpleGrantedAuthority(normalizeRole(role)));
        }

        return new UsernamePasswordAuthenticationToken(userId.trim(), "N/A", authorities);
    }

    private static List<String> splitRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return List.of();
        }
        return Arrays.stream(rolesHeader.split("[,;\\s]+"))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .toList();
    }

    private static String normalizeRole(String role) {
        String normalized = role.toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }
}