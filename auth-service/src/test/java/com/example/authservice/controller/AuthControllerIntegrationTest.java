package com.example.authservice.controller;

import com.example.authservice.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for AuthController — all endpoints, multiple scenarios.
 * Runs a real Spring context on a random port with H2 in-memory DB.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerIntegrationTest {

    @LocalServerPort
    private int port;

    private RestTemplate restTemplate;

    private String url(String path) {
        return "http://localhost:" + port + "/auth" + path;
    }

    @BeforeEach
    void setUpRestTemplate() {
        restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        // Don't throw exceptions on 4xx/5xx — let tests assert on status code
        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            public boolean hasError(ClientHttpResponse r) throws IOException { return false; }
            public void handleError(ClientHttpResponse r) throws IOException { }
        });
    }

    /** Register a fresh user and return the full login response. */
    private LoginResponse registerFresh() {
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        RegisterRequest req = RegisterRequest.builder()
                .username("user_" + uid)
                .email(uid + "@test.com")
                .password("Password1!")
                .fullName("Test " + uid)
                .build();
        ResponseEntity<LoginResponse> resp = restTemplate.postForEntity(url("/register"), req, LoginResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /health
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    void health_alwaysReturns200WithStatusUp() {
        ResponseEntity<Map> resp = restTemplate.getForEntity(url("/health"), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("status", "UP");
        assertThat(resp.getBody()).containsEntry("service", "auth-service");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /register
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(2)
    void register_newUser_returns201WithTokensAndUsername() {
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        RegisterRequest req = RegisterRequest.builder()
                .username("reg_" + uid)
                .email("reg_" + uid + "@test.com")
                .password("Password1!")
                .fullName("Reg User")
                .build();

        ResponseEntity<LoginResponse> resp = restTemplate.postForEntity(url("/register"), req, LoginResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        LoginResponse body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getAccessToken()).isNotBlank();
        assertThat(body.getRefreshToken()).isNotBlank();
        assertThat(body.getToken()).isNotBlank();          // legacy alias
        assertThat(body.getUsername()).isEqualTo("reg_" + uid);
        assertThat(body.getRole()).isNotNull();
    }

    @Test
    @Order(3)
    void register_duplicateEmail_returns409Conflict() {
        String email = "dup_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + "@test.com";
        RegisterRequest r1 = RegisterRequest.builder().username("u1_" + UUID.randomUUID())
                .email(email).password("Pass1!").fullName("A").build();
        restTemplate.postForEntity(url("/register"), r1, LoginResponse.class);

        RegisterRequest r2 = RegisterRequest.builder().username("u2_" + UUID.randomUUID())
                .email(email).password("Pass1!").fullName("B").build();
        ResponseEntity<Map> resp = restTemplate.postForEntity(url("/register"), r2, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @Order(4)
    void register_duplicateUsername_returns409Conflict() {
        String uname = "uname_" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        RegisterRequest r1 = RegisterRequest.builder().username(uname)
                .email(UUID.randomUUID() + "@test.com").password("Pass1!").fullName("A").build();
        restTemplate.postForEntity(url("/register"), r1, LoginResponse.class);

        RegisterRequest r2 = RegisterRequest.builder().username(uname)
                .email(UUID.randomUUID() + "@test.com").password("Pass2!").fullName("B").build();
        ResponseEntity<Map> resp = restTemplate.postForEntity(url("/register"), r2, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @Order(5)
    void register_shortPassword_returns400() {
        RegisterRequest req = RegisterRequest.builder().username("short_" + UUID.randomUUID())
                .email(UUID.randomUUID() + "@test.com").password("abc").fullName("Test").build();
        ResponseEntity<Map> resp = restTemplate.postForEntity(url("/register"), req, Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(6)
    void register_missingEmail_returns400() {
        RegisterRequest req = RegisterRequest.builder().username("noemail_" + UUID.randomUUID())
                .email("").password("Password1!").fullName("Test").build();
        ResponseEntity<Map> resp = restTemplate.postForEntity(url("/register"), req, Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(7)
    void register_missingUsername_returns400() {
        RegisterRequest req = RegisterRequest.builder().username("")
                .email(UUID.randomUUID() + "@test.com").password("Password1!").fullName("Test").build();
        ResponseEntity<Map> resp = restTemplate.postForEntity(url("/register"), req, Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /login
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(8)
    void login_validCredentials_returns200WithBothTokens() {
        LoginResponse registered = registerFresh();
        // extract email from token — easier to just re-register but re-use username
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String email = uid + "@login.test";
        RegisterRequest reg = RegisterRequest.builder().username("ln_" + uid)
                .email(email).password("LoginPass1!").fullName("Login User").build();
        restTemplate.postForEntity(url("/register"), reg, LoginResponse.class);

        LoginRequest login = LoginRequest.builder().email(email).password("LoginPass1!").build();
        ResponseEntity<LoginResponse> resp = restTemplate.postForEntity(url("/login"), login, LoginResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getAccessToken()).isNotBlank();
        assertThat(resp.getBody().getRefreshToken()).isNotBlank();
        assertThat(resp.getBody().getUsername()).isEqualTo("ln_" + uid);
    }

    @Test
    @Order(9)
    void login_wrongPassword_returns401() {
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String email = uid + "@login.test";
        RegisterRequest reg = RegisterRequest.builder().username("wp_" + uid)
                .email(email).password("CorrectPass1!").fullName("User").build();
        restTemplate.postForEntity(url("/register"), reg, LoginResponse.class);

        LoginRequest login = LoginRequest.builder().email(email).password("WrongPass!").build();
        ResponseEntity<Map> resp = restTemplate.postForEntity(url("/login"), login, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(10)
    void login_unknownEmail_returns401() {
        LoginRequest login = LoginRequest.builder().email("nobody_" + UUID.randomUUID() + "@test.com")
                .password("SomePass1!").build();
        ResponseEntity<Map> resp = restTemplate.postForEntity(url("/login"), login, Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(11)
    void login_preSeededAdmin_returns200() {
        LoginRequest login = LoginRequest.builder().email("admin@local.test").password("Admin@12345").build();
        ResponseEntity<LoginResponse> resp = restTemplate.postForEntity(url("/login"), login, LoginResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getRole().name()).isEqualTo("ADMIN");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /validate
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(12)
    void validate_validToken_returns200WithUserInfo() {
        LoginResponse reg = registerFresh();
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(reg.getAccessToken());

        ResponseEntity<Map> resp = restTemplate.exchange(url("/validate"), HttpMethod.POST,
                new HttpEntity<>(h), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsKey("valid");
        assertThat(resp.getBody().get("valid")).isEqualTo(true);
        assertThat(resp.getBody()).containsKey("username");
        assertThat(resp.getBody()).containsKey("role");
    }

    @Test
    @Order(13)
    void validate_invalidToken_returns401() {
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", "Bearer not.a.real.token");

        ResponseEntity<Map> resp = restTemplate.exchange(url("/validate"), HttpMethod.POST,
                new HttpEntity<>(h), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        if (resp.getBody() != null) {
            assertThat(resp.getBody().get("valid")).isEqualTo(false);
        }
    }

    @Test
    @Order(14)
    void validate_missingAuthHeader_returns401() {
        ResponseEntity<Map> resp = restTemplate.exchange(url("/validate"), HttpMethod.POST,
                new HttpEntity<>(new HttpHeaders()), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /refresh
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(15)
    void refresh_validRefreshToken_returns200WithNewTokens() {
        LoginResponse reg = registerFresh();
        RefreshRequest req = RefreshRequest.builder().refreshToken(reg.getRefreshToken()).build();

        ResponseEntity<LoginResponse> resp = restTemplate.postForEntity(url("/refresh"), req, LoginResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getAccessToken()).isNotBlank();
        assertThat(resp.getBody().getRefreshToken()).isNotBlank();
    }

    @Test
    @Order(16)
    void refresh_invalidToken_returns401() {
        RefreshRequest req = RefreshRequest.builder().refreshToken("totally-fake-refresh-token").build();
        ResponseEntity<Map> resp = restTemplate.postForEntity(url("/refresh"), req, Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(17)
    void refresh_revokedToken_returns401() {
        LoginResponse reg = registerFresh();
        // Logout to revoke
        RefreshRequest logoutReq = RefreshRequest.builder().refreshToken(reg.getRefreshToken()).build();
        restTemplate.postForEntity(url("/logout"), logoutReq, Map.class);

        // Now refresh with the revoked token
        RefreshRequest refreshReq = RefreshRequest.builder().refreshToken(reg.getRefreshToken()).build();
        ResponseEntity<Map> resp = restTemplate.postForEntity(url("/refresh"), refreshReq, Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /logout
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(18)
    void logout_validToken_returns200() {
        LoginResponse reg = registerFresh();
        RefreshRequest req = RefreshRequest.builder().refreshToken(reg.getRefreshToken()).build();

        ResponseEntity<Map> resp = restTemplate.postForEntity(url("/logout"), req, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsKey("message");
    }

    @Test
    @Order(19)
    void logout_alreadyRevoked_stillReturns200() {
        LoginResponse reg = registerFresh();
        RefreshRequest req = RefreshRequest.builder().refreshToken(reg.getRefreshToken()).build();
        restTemplate.postForEntity(url("/logout"), req, Map.class); // first logout

        ResponseEntity<Map> resp = restTemplate.postForEntity(url("/logout"), req, Map.class); // second logout
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /change-password
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(20)
    void changePassword_correctOldPassword_returns200() {
        LoginResponse reg = registerFresh();
        ChangePasswordRequest req = ChangePasswordRequest.builder()
                .oldPassword("Password1!").newPassword("NewPass2!").build();
        HttpEntity<ChangePasswordRequest> entity = new HttpEntity<>(req, bearerHeaders(reg.getAccessToken()));

        ResponseEntity<Map> resp = restTemplate.exchange(url("/change-password"), HttpMethod.POST, entity, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsKey("message");
    }

    @Test
    @Order(21)
    void changePassword_wrongOldPassword_returns401() {
        LoginResponse reg = registerFresh();
        ChangePasswordRequest req = ChangePasswordRequest.builder()
                .oldPassword("WRONG_PASSWORD").newPassword("NewPass2!").build();
        HttpEntity<ChangePasswordRequest> entity = new HttpEntity<>(req, bearerHeaders(reg.getAccessToken()));

        ResponseEntity<Map> resp = restTemplate.exchange(url("/change-password"), HttpMethod.POST, entity, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(22)
    void changePassword_unauthenticated_returns401() {
        ChangePasswordRequest req = ChangePasswordRequest.builder()
                .oldPassword("Password1!").newPassword("NewPass2!").build();
        ResponseEntity<Map> resp = restTemplate.postForEntity(url("/change-password"), req, Map.class);
        assertThat(resp.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /forgot-password & /reset-password
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(23)
    void forgotPassword_existingEmail_returns200WithResetToken() {
        LoginResponse reg = registerFresh();
        // We need the email — register another user with known email
        String email = "forgot_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + "@test.com";
        RegisterRequest regReq = RegisterRequest.builder()
                .username("fp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8))
                .email(email).password("Password1!").fullName("Forgot User").build();
        restTemplate.postForEntity(url("/register"), regReq, LoginResponse.class);

        ForgotPasswordRequest req = ForgotPasswordRequest.builder().email(email).build();
        ResponseEntity<Map> resp = restTemplate.postForEntity(url("/forgot-password"), req, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsKey("resetToken");
        assertThat((String) resp.getBody().get("resetToken")).isNotBlank();
    }

    @Test
    @Order(24)
    void forgotPassword_unknownEmail_returns401() {
        ForgotPasswordRequest req = ForgotPasswordRequest.builder()
                .email("nobody_" + UUID.randomUUID() + "@ghost.test").build();
        ResponseEntity<Map> resp = restTemplate.postForEntity(url("/forgot-password"), req, Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(25)
    void resetPassword_validToken_returns200ThenOldPasswordInvalid() {
        String email = "reset_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + "@test.com";
        String uname = "rst_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        RegisterRequest regReq = RegisterRequest.builder()
                .username(uname).email(email).password("OldPass1!").fullName("Reset User").build();
        restTemplate.postForEntity(url("/register"), regReq, LoginResponse.class);

        // Get reset token
        ForgotPasswordRequest forgot = ForgotPasswordRequest.builder().email(email).build();
        Map forgotResp = restTemplate.postForEntity(url("/forgot-password"), forgot, Map.class).getBody();
        String resetToken = (String) forgotResp.get("resetToken");

        // Reset password
        ResetPasswordRequest resetReq = ResetPasswordRequest.builder()
                .token(resetToken).newPassword("NewPass2!").build();
        ResponseEntity<Map> resetResp = restTemplate.postForEntity(url("/reset-password"), resetReq, Map.class);
        assertThat(resetResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Old password should no longer work
        LoginRequest loginOld = LoginRequest.builder().email(email).password("OldPass1!").build();
        ResponseEntity<Map> loginResp = restTemplate.postForEntity(url("/login"), loginOld, Map.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // New password should work
        LoginRequest loginNew = LoginRequest.builder().email(email).password("NewPass2!").build();
        ResponseEntity<LoginResponse> loginOkResp = restTemplate.postForEntity(url("/login"), loginNew, LoginResponse.class);
        assertThat(loginOkResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(26)
    void resetPassword_invalidToken_returns401() {
        ResetPasswordRequest req = ResetPasswordRequest.builder()
                .token("completely-invalid-token").newPassword("NewPass2!").build();
        ResponseEntity<Map> resp = restTemplate.postForEntity(url("/reset-password"), req, Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(27)
    void resetPassword_usedToken_returns401() {
        String email = "rst2_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + "@test.com";
        RegisterRequest regReq = RegisterRequest.builder()
                .username("rst2_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8))
                .email(email).password("OldPass1!").fullName("Reset User 2").build();
        restTemplate.postForEntity(url("/register"), regReq, LoginResponse.class);

        Map forgotResp = restTemplate.postForEntity(url("/forgot-password"),
                ForgotPasswordRequest.builder().email(email).build(), Map.class).getBody();
        String resetToken = (String) forgotResp.get("resetToken");

        // First use — OK
        restTemplate.postForEntity(url("/reset-password"),
                ResetPasswordRequest.builder().token(resetToken).newPassword("Pass2!XX").build(), Map.class);

        // Second use — should fail (token is marked used)
        ResponseEntity<Map> resp = restTemplate.postForEntity(url("/reset-password"),
                ResetPasswordRequest.builder().token(resetToken).newPassword("Pass3!YY").build(), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
