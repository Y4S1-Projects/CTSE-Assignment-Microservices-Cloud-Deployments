package com.example.apigateway.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JwtTokenValidator (API Gateway).
 */
@ExtendWith(MockitoExtension.class)
class JwtTokenValidatorTest {

    @InjectMocks
    private JwtTokenValidator jwtTokenValidator;

    private static final String SECRET = "test-super-secret-jwt-key-minimum-32-characters-needed-for-hmac";
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtTokenValidator, "jwtSecret", SECRET);
        signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    /** Build a token signed with the same key as the validator. */
    private String buildToken(String userId, String username, String role, long expiryMs) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(signingKey)
                .compact();
    }

    // ── validateToken ──────────────────────────────────────────────────────────

    @Test
    void validateToken_validToken_returnsTrue() {
        String token = buildToken("u1", "alice", "CUSTOMER", 900_000);
        assertThat(jwtTokenValidator.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_expiredToken_returnsFalse() {
        String token = buildToken("u1", "alice", "CUSTOMER", -1000); // already expired
        assertThat(jwtTokenValidator.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_tokenSignedWithWrongKey_returnsFalse() {
        // Sign with a different key
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "wrong-key-that-is-different-but-still-32-chars-long!!".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("alice")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith(wrongKey)
                .compact();
        assertThat(jwtTokenValidator.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_malformedToken_returnsFalse() {
        assertThat(jwtTokenValidator.validateToken("not.a.valid.jwt")).isFalse();
    }

    @Test
    void validateToken_null_returnsFalse() {
        assertThat(jwtTokenValidator.validateToken(null)).isFalse();
    }

    @Test
    void validateToken_emptyString_returnsFalse() {
        assertThat(jwtTokenValidator.validateToken("")).isFalse();
    }

    @Test
    void validateToken_tamperedPayload_returnsFalse() {
        String token = buildToken("u1", "alice", "CUSTOMER", 900_000);
        // Split and corrupt the payload (middle) section
        String[] parts = token.split("\\.");
        // append garbage to the signature to corrupt it
        String corrupted = parts[0] + "." + parts[1] + "." + parts[2].substring(0, parts[2].length() - 1) + "X";
        assertThat(jwtTokenValidator.validateToken(corrupted)).isFalse();
    }

    // ── extractUserId ──────────────────────────────────────────────────────────

    @Test
    void extractUserId_validToken_returnsCorrectId() {
        String token = buildToken("user-abc-123", "alice", "CUSTOMER", 900_000);
        assertThat(jwtTokenValidator.extractUserId(token)).isEqualTo("user-abc-123");
    }

    // ── extractUsername ────────────────────────────────────────────────────────

    @Test
    void extractUsername_validToken_returnsCorrectUsername() {
        String token = buildToken("u1", "charlie", "ADMIN", 900_000);
        assertThat(jwtTokenValidator.extractUsername(token)).isEqualTo("charlie");
    }

    // ── extractRoles ──────────────────────────────────────────────────────────

    @Test
    void extractRoles_singleRoleStringClaim_returnsListWithOneRole() {
        String token = buildToken("u1", "alice", "CUSTOMER", 900_000);
        List<String> roles = jwtTokenValidator.extractRoles(token);
        assertThat(roles).containsExactly("CUSTOMER");
    }

    @Test
    void extractRoles_adminRole_returnsListWithAdmin() {
        String token = buildToken("a1", "admin", "ADMIN", 900_000);
        List<String> roles = jwtTokenValidator.extractRoles(token);
        assertThat(roles).containsExactly("ADMIN");
    }

    @Test
    void extractRoles_tokenWithRolesListClaim_returnsRolesList() {
        // Build a token that uses the old "roles" list claim format
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "u1");
        claims.put("username", "alice");
        claims.put("roles", List.of("CUSTOMER", "MANAGER"));

        String token = Jwts.builder()
                .claims(claims)
                .subject("alice")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith(signingKey)
                .compact();

        List<String> roles = jwtTokenValidator.extractRoles(token);
        assertThat(roles).contains("CUSTOMER", "MANAGER");
    }

    // ── isTokenExpired ─────────────────────────────────────────────────────────

    @Test
    void isTokenExpired_freshToken_returnsFalse() {
        String token = buildToken("u1", "alice", "CUSTOMER", 900_000);
        assertThat(jwtTokenValidator.isTokenExpired(token)).isFalse();
    }

    @Test
    void isTokenExpired_expiredToken_returnsTrue() {
        String token = buildToken("u1", "alice", "CUSTOMER", -1000);
        assertThat(jwtTokenValidator.isTokenExpired(token)).isTrue();
    }
}
