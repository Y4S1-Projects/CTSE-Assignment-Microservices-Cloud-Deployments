package com.example.apigateway.filter;

import com.example.apigateway.util.JwtTokenValidator;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtAuthenticationFilter (API Gateway global filter).
 * Tests public-endpoint bypass logic and JWT validation enforcement.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private JwtTokenValidator jwtTokenValidator;

    private static final String SECRET = "test-super-secret-jwt-key-minimum-32-characters-needed-for-hmac";
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    /** Build a signed JWT for testing. */
    private String buildValidToken(String userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith(signingKey)
                .compact();
    }

    private GatewayFilterChain passThroughChain() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        lenient().when(chain.filter(any())).thenReturn(Mono.empty());
        return chain;
    }

    // ── Public endpoints bypass ────────────────────────────────────────────────

    @Test
    void publicEndpoint_authRegister_bypassesFilter() {
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/auth/register")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = passThroughChain();

        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();
        verify(chain).filter(any());
        verify(jwtTokenValidator, never()).validateToken(any());
    }

    @Test
    void publicEndpoint_authLogin_bypassesFilter() {
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/auth/login")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = passThroughChain();

        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();
        verify(chain).filter(any());
        verify(jwtTokenValidator, never()).validateToken(any());
    }

    @Test
    void publicEndpoint_authRefresh_bypassesFilter() {
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/auth/refresh")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = passThroughChain();

        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();
        verify(chain).filter(any());
        verify(jwtTokenValidator, never()).validateToken(any());
    }

    @Test
    void publicEndpoint_authForgotPassword_bypassesFilter() {
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/auth/forgot-password")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = passThroughChain();

        jwtAuthenticationFilter.filter(exchange, chain);

        verify(jwtTokenValidator, never()).validateToken(any());
    }

    @Test
    void publicEndpoint_authResetPassword_bypassesFilter() {
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/auth/reset-password")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = passThroughChain();

        jwtAuthenticationFilter.filter(exchange, chain);

        verify(jwtTokenValidator, never()).validateToken(any());
    }

    @Test
    void publicEndpoint_authHealth_bypassesFilter() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/auth/health")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = passThroughChain();

        jwtAuthenticationFilter.filter(exchange, chain);

        verify(jwtTokenValidator, never()).validateToken(any());
    }

    @Test
    void publicEndpoint_swaggerUi_bypassesFilter() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/swagger-ui/index.html")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = passThroughChain();

        jwtAuthenticationFilter.filter(exchange, chain);

        verify(jwtTokenValidator, never()).validateToken(any());
    }

    @Test
    void publicEndpoint_apiDocs_bypassesFilter() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/v3/api-docs")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = passThroughChain();

        jwtAuthenticationFilter.filter(exchange, chain);

        verify(jwtTokenValidator, never()).validateToken(any());
    }

    @Test
    void publicEndpoint_authValidate_bypassesFilter() {
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/auth/validate")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = passThroughChain();

        jwtAuthenticationFilter.filter(exchange, chain);

        verify(jwtTokenValidator, never()).validateToken(any());
    }

    // ── Protected endpoints with no token ─────────────────────────────────────

    @Test
    void protectedEndpoint_noAuthHeader_returns401() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/auth/users/me")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = passThroughChain();

        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);
        StepVerifier.create(result).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void protectedEndpoint_malformedAuthHeader_noBearerPrefix_returns401() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/auth/users/me")
                .header("Authorization", "Basic dXNlcjpwYXNz")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = passThroughChain();

        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);
        StepVerifier.create(result).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    // ── Protected endpoints with invalid token ─────────────────────────────────

    @Test
    void protectedEndpoint_invalidToken_returns401() {
        when(jwtTokenValidator.validateToken("bad.token.here")).thenReturn(false);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/auth/users/me")
                .header("Authorization", "Bearer bad.token.here")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = passThroughChain();

        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);
        StepVerifier.create(result).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    // ── Protected endpoints with valid token ───────────────────────────────────

    @Test
    void protectedEndpoint_validToken_injectsHeadersAndCallsChain() {
        String validToken = buildValidToken("user-123", "alice", "CUSTOMER");
        when(jwtTokenValidator.validateToken(validToken)).thenReturn(true);
        when(jwtTokenValidator.extractUserId(validToken)).thenReturn("user-123");
        when(jwtTokenValidator.extractUsername(validToken)).thenReturn("alice");
        when(jwtTokenValidator.extractRoles(validToken)).thenReturn(java.util.List.of("CUSTOMER"));

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/auth/users/me")
                .header("Authorization", "Bearer " + validToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Capture the mutated exchange passed to the chain
        ArgumentCaptor<ServerWebExchange> exchangeCaptor = ArgumentCaptor.forClass(ServerWebExchange.class);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchangeCaptor.capture())).thenReturn(Mono.empty());

        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);
        StepVerifier.create(result).verifyComplete();

        verify(chain).filter(any());

        // Downstream request must contain injected headers
        HttpHeaders downstreamHeaders = exchangeCaptor.getValue().getRequest().getHeaders();
        assertThat(downstreamHeaders.getFirst("X-User-Id")).isEqualTo("user-123");
        assertThat(downstreamHeaders.getFirst("X-Username")).isEqualTo("alice");
        assertThat(downstreamHeaders.getFirst("X-User-Roles")).contains("CUSTOMER");
    }

    @Test
    void protectedEndpoint_adminToken_injectsAdminRole() {
        String adminToken = buildValidToken("admin-1", "admin", "ADMIN");
        when(jwtTokenValidator.validateToken(adminToken)).thenReturn(true);
        when(jwtTokenValidator.extractUserId(adminToken)).thenReturn("admin-1");
        when(jwtTokenValidator.extractUsername(adminToken)).thenReturn("admin");
        when(jwtTokenValidator.extractRoles(adminToken)).thenReturn(java.util.List.of("ADMIN"));

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/auth/admin/users")
                .header("Authorization", "Bearer " + adminToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, chain)).verifyComplete();

        assertThat(captor.getValue().getRequest().getHeaders().getFirst("X-User-Roles")).contains("ADMIN");
    }

    // ── Filter ordering ────────────────────────────────────────────────────────

    @Test
    void getOrder_returnsMinusOne() {
        assertThat(jwtAuthenticationFilter.getOrder()).isEqualTo(-1);
    }
}
