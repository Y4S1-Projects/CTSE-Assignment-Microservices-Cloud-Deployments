package com.example.authservice.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    private static final String SECRET = "test-super-secret-jwt-key-minimum-32-characters-needed-for-hmac";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", 900000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtIssuer", "food-ordering-system");
    }

    // ── generateToken ──────────────────────────────────────────────────────────

    @Test
    void generateToken_returnsNonNullNonBlankString() {
        String token = jwtTokenProvider.generateToken("user-1", "alice", "CUSTOMER");
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void generateToken_differentUsers_produceDifferentTokens() {
        String t1 = jwtTokenProvider.generateToken("user-1", "alice", "CUSTOMER");
        String t2 = jwtTokenProvider.generateToken("user-2", "bob", "ADMIN");
        assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    void generateToken_sameUser_differentRoles_differentTokens() {
        String t1 = jwtTokenProvider.generateToken("user-1", "alice", "CUSTOMER");
        String t2 = jwtTokenProvider.generateToken("user-1", "alice", "ADMIN");
        assertThat(t1).isNotEqualTo(t2);
    }

    // ── validateToken ──────────────────────────────────────────────────────────

    @Test
    void validateToken_freshToken_returnsTrue() {
        String token = jwtTokenProvider.generateToken("user-1", "alice", "CUSTOMER");
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_randomGarbage_returnsFalse() {
        assertThat(jwtTokenProvider.validateToken("not.a.jwt.string")).isFalse();
    }

    @Test
    void validateToken_null_returnsFalse() {
        assertThat(jwtTokenProvider.validateToken(null)).isFalse();
    }

    @Test
    void validateToken_emptyString_returnsFalse() {
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
    }

    @Test
    void validateToken_tamperedSignature_returnsFalse() {
        String token = jwtTokenProvider.generateToken("user-1", "alice", "CUSTOMER");
        // Replace last char to corrupt the signature
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");
        assertThat(jwtTokenProvider.validateToken(tampered)).isFalse();
    }

    @Test
    void validateToken_expiredToken_returnsFalse() throws InterruptedException {
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", 1L); // 1 ms
        String token = jwtTokenProvider.generateToken("user-1", "alice", "CUSTOMER");
        Thread.sleep(50); // ensure expiry
        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }

    // ── extractUsername ────────────────────────────────────────────────────────

    @Test
    void extractUsername_returnsCorrectValue() {
        String token = jwtTokenProvider.generateToken("user-1", "alice", "CUSTOMER");
        assertThat(jwtTokenProvider.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void extractUsername_adminUser_returnsAdminUsername() {
        String token = jwtTokenProvider.generateToken("admin-1", "admin", "ADMIN");
        assertThat(jwtTokenProvider.extractUsername(token)).isEqualTo("admin");
    }

    // ── extractUserId ──────────────────────────────────────────────────────────

    @Test
    void extractUserId_returnsCorrectValue() {
        String token = jwtTokenProvider.generateToken("user-abc-123", "alice", "CUSTOMER");
        assertThat(jwtTokenProvider.extractUserId(token)).isEqualTo("user-abc-123");
    }

    // ── extractRole ────────────────────────────────────────────────────────────

    @Test
    void extractRole_customerRole_returnsCustomer() {
        String token = jwtTokenProvider.generateToken("user-1", "alice", "CUSTOMER");
        assertThat(jwtTokenProvider.extractRole(token)).isEqualTo("CUSTOMER");
    }

    @Test
    void extractRole_adminRole_returnsAdmin() {
        String token = jwtTokenProvider.generateToken("admin-1", "admin", "ADMIN");
        assertThat(jwtTokenProvider.extractRole(token)).isEqualTo("ADMIN");
    }

    // ── isTokenExpired ─────────────────────────────────────────────────────────

    @Test
    void isTokenExpired_freshToken_returnsFalse() {
        String token = jwtTokenProvider.generateToken("user-1", "alice", "CUSTOMER");
        assertThat(jwtTokenProvider.isTokenExpired(token)).isFalse();
    }

    @Test
    void isTokenExpired_expiredToken_returnsTrue() throws InterruptedException {
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", 1L);
        String token = jwtTokenProvider.generateToken("user-1", "alice", "CUSTOMER");
        Thread.sleep(50);
        assertThat(jwtTokenProvider.isTokenExpired(token)).isTrue();
    }

    // ── round-trip consistency ─────────────────────────────────────────────────

    @Test
    void generateAndExtract_allClaimsRoundTrip() {
        String token = jwtTokenProvider.generateToken("user-xyz", "charlie", "ADMIN");
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.extractUserId(token)).isEqualTo("user-xyz");
        assertThat(jwtTokenProvider.extractUsername(token)).isEqualTo("charlie");
        assertThat(jwtTokenProvider.extractRole(token)).isEqualTo("ADMIN");
        assertThat(jwtTokenProvider.isTokenExpired(token)).isFalse();
    }
}
