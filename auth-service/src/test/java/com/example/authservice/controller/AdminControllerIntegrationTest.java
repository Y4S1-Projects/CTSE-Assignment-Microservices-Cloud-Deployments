package com.example.authservice.controller;

import com.example.authservice.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for AdminController — all endpoints, RBAC enforcement.
 * Bootstrap admin (admin@local.test / Admin@12345) is available via AdminBootstrapConfig.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminControllerIntegrationTest {

    @LocalServerPort
    private int port;

        private RestTemplate restTemplate;

    private String url(String path) {
        return "http://localhost:" + port + "/auth" + path;
    }

        @BeforeEach
        void setUpRestTemplate() {
                restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
                restTemplate.setErrorHandler(new ResponseErrorHandler() {
                        public boolean hasError(ClientHttpResponse r) throws IOException { return false; }
                        public void handleError(ClientHttpResponse r) throws IOException { }
                });
        }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    /** Get an admin access token using the pre-seeded bootstrap admin. */
    private String adminToken() {
        LoginRequest req = LoginRequest.builder().email("admin@local.test").password("Admin@12345").build();
        ResponseEntity<LoginResponse> resp = restTemplate.postForEntity(url("/login"), req, LoginResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody().getAccessToken();
    }

    /** Register a fresh customer and return their access token. */
    private String customerToken() {
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        RegisterRequest req = RegisterRequest.builder()
                .username("cst_" + uid)
                .email("cst_" + uid + "@test.com")
                .password("Pass1!")
                .fullName("Customer " + uid)
                .build();
        ResponseEntity<LoginResponse> resp = restTemplate.postForEntity(url("/register"), req, LoginResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().getAccessToken();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /admin/users — list all users
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void listUsers_asAdmin_returns200WithUserList() {
        HttpEntity<Void> entity = new HttpEntity<>(bearerHeaders(adminToken()));
        ResponseEntity<List> resp = restTemplate.exchange(url("/admin/users"), HttpMethod.GET, entity, List.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        // At minimum the admin user should be present
        assertThat(resp.getBody().size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void listUsers_asCustomer_returns403Forbidden() {
        HttpEntity<Void> entity = new HttpEntity<>(bearerHeaders(customerToken()));
        ResponseEntity<Map> resp = restTemplate.exchange(url("/admin/users"), HttpMethod.GET, entity, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void listUsers_unauthenticated_returns401Or403() {
        ResponseEntity<Map> resp = restTemplate.exchange(url("/admin/users"), HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()), Map.class);
        assertThat(resp.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /admin/users — create user
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void createUser_asAdmin_returns201WithUserData() {
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        AdminCreateUserRequest req = AdminCreateUserRequest.builder()
                .username("admin_created_" + uid)
                .email("admin_created_" + uid + "@test.com")
                .fullName("Admin Created User")
                .password("Password1!")
                .build();  // role defaults to CUSTOMER in AdminController
        HttpEntity<AdminCreateUserRequest> entity = new HttpEntity<>(req, bearerHeaders(adminToken()));

        ResponseEntity<Map> resp = restTemplate.exchange(url("/admin/users"), HttpMethod.POST, entity, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().get("username")).isEqualTo("admin_created_" + uid);
        assertThat(resp.getBody()).containsKey("id");
    }

    @Test
    void createAdminUser_asAdmin_returns201WithAdminRole() {
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        AdminCreateUserRequest req = AdminCreateUserRequest.builder()
                .username("new_admin_" + uid)
                .email("new_admin_" + uid + "@test.com")
                .fullName("New Admin")
                .password("Password1!")
                .role(com.example.authservice.entity.Role.ADMIN)
                .build();
        HttpEntity<AdminCreateUserRequest> entity = new HttpEntity<>(req, bearerHeaders(adminToken()));

        ResponseEntity<Map> resp = restTemplate.exchange(url("/admin/users"), HttpMethod.POST, entity, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().get("role")).isEqualTo("ADMIN");
    }

    @Test
    void createUser_asCustomer_returns403() {
        AdminCreateUserRequest req = AdminCreateUserRequest.builder()
                .username("should_fail").email("fail@test.com")
                .fullName("Fail").password("Password1!").build();
        HttpEntity<AdminCreateUserRequest> entity = new HttpEntity<>(req, bearerHeaders(customerToken()));

        ResponseEntity<Map> resp = restTemplate.exchange(url("/admin/users"), HttpMethod.POST, entity, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /admin/users/{id} — get by ID
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void getUserById_asAdmin_returns200() {
        // Create user first
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        AdminCreateUserRequest createReq = AdminCreateUserRequest.builder()
                .username("byid_" + uid).email("byid_" + uid + "@test.com")
                .fullName("By ID User").password("Password1!").build();
        String adminTok = adminToken();
        ResponseEntity<Map> created = restTemplate.exchange(url("/admin/users"), HttpMethod.POST,
                new HttpEntity<>(createReq, bearerHeaders(adminTok)), Map.class);
        String userId = (String) created.getBody().get("id");

        // Fetch by id
        ResponseEntity<Map> resp = restTemplate.exchange(url("/admin/users/" + userId), HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminTok)), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("id")).isEqualTo(userId);
        assertThat(resp.getBody().get("username")).isEqualTo("byid_" + uid);
    }

    @Test
    void getUserById_notFound_returns400() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                url("/admin/users/00000000-0000-0000-0000-000000000000"), HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminToken())), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getUserById_asCustomer_returns403() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                url("/admin/users/some-id"), HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(customerToken())), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PATCH /admin/users/{id}/status — activate / deactivate
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void deactivateUser_asAdmin_returns200AndUserCannotLogin() {
        // Register a normal customer
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String email = "deactivate_" + uid + "@test.com";
        RegisterRequest regReq = RegisterRequest.builder()
                .username("deact_" + uid).email(email).password("Password1!").fullName("Deactivate Me").build();
        ResponseEntity<LoginResponse> regResp = restTemplate.postForEntity(url("/register"), regReq, LoginResponse.class);
        String userId = regResp.getBody().getUserId();
        String adminTok = adminToken();

        // Deactivate
        UpdateUserStatusRequest statusReq = UpdateUserStatusRequest.builder().active(false).build();
        ResponseEntity<Map> statusResp = restTemplate.exchange(
                url("/admin/users/" + userId + "/status"), HttpMethod.PATCH,
                new HttpEntity<>(statusReq, bearerHeaders(adminTok)), Map.class);

        assertThat(statusResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(statusResp.getBody().get("active")).isEqualTo(false);

        // Verify user can no longer login
        LoginRequest loginReq = LoginRequest.builder().email(email).password("Password1!").build();
        ResponseEntity<Map> loginResp = restTemplate.postForEntity(url("/login"), loginReq, Map.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void reactivateUser_asAdmin_returns200AndUserCanLogin() {
        // Register + deactivate + reactivate
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String email = "react_" + uid + "@test.com";
        RegisterRequest regReq = RegisterRequest.builder()
                .username("react_" + uid).email(email).password("Pass1!").fullName("Reactivate Me").build();
        ResponseEntity<LoginResponse> regResp = restTemplate.postForEntity(url("/register"), regReq, LoginResponse.class);
        String userId = regResp.getBody().getUserId();
        String adminTok = adminToken();

        // Deactivate
        restTemplate.exchange(url("/admin/users/" + userId + "/status"), HttpMethod.PATCH,
                new HttpEntity<>(UpdateUserStatusRequest.builder().active(false).build(), bearerHeaders(adminTok)), Map.class);

        // Reactivate
        ResponseEntity<Map> resp = restTemplate.exchange(url("/admin/users/" + userId + "/status"), HttpMethod.PATCH,
                new HttpEntity<>(UpdateUserStatusRequest.builder().active(true).build(), bearerHeaders(adminTok)), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("active")).isEqualTo(true);

        // User can login again
        LoginRequest loginReq = LoginRequest.builder().email(email).password("Pass1!").build();
        ResponseEntity<LoginResponse> loginResp = restTemplate.postForEntity(url("/login"), loginReq, LoginResponse.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateStatus_asCustomer_returns403() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                url("/admin/users/some-id/status"), HttpMethod.PATCH,
                new HttpEntity<>(UpdateUserStatusRequest.builder().active(false).build(), bearerHeaders(customerToken())),
                Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DELETE /admin/users/{id}
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void deleteUser_asAdmin_returns204AndUserGone() {
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        AdminCreateUserRequest createReq = AdminCreateUserRequest.builder()
                .username("del_" + uid).email("del_" + uid + "@test.com")
                .fullName("To Delete").password("Password1!").build();
        String adminTok = adminToken();
        ResponseEntity<Map> created = restTemplate.exchange(url("/admin/users"), HttpMethod.POST,
                new HttpEntity<>(createReq, bearerHeaders(adminTok)), Map.class);
        String userId = (String) created.getBody().get("id");

        // Delete
        ResponseEntity<Void> delResp = restTemplate.exchange(url("/admin/users/" + userId),
                HttpMethod.DELETE, new HttpEntity<>(bearerHeaders(adminTok)), Void.class);
        assertThat(delResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify gone — fetch returns 400
        ResponseEntity<Map> getResp = restTemplate.exchange(url("/admin/users/" + userId),
                HttpMethod.GET, new HttpEntity<>(bearerHeaders(adminTok)), Map.class);
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void deleteUser_asCustomer_returns403() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                url("/admin/users/some-id"), HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(customerToken())), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deleteUser_unauthenticated_returns401Or403() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                url("/admin/users/some-id"), HttpMethod.DELETE,
                new HttpEntity<>(new HttpHeaders()), Map.class);
        assertThat(resp.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RBAC cross-checks: admin can access own data, customer cannot access admin area
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void rbac_customerCanUseUserEndpoints_butNotAdminEndpoints() {
        String custTok = customerToken();

        // /users/me — OK for customer
        ResponseEntity<Map> meResp = restTemplate.exchange(url("/users/me"), HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(custTok)), Map.class);
        assertThat(meResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // /admin/users — Forbidden for customer
        ResponseEntity<Map> adminResp = restTemplate.exchange(url("/admin/users"), HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(custTok)), Map.class);
        assertThat(adminResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void rbac_adminCanAccessBothUserAndAdminEndpoints() {
        String adminTok = adminToken();

        // /admin/users — OK for admin
        ResponseEntity<List> adminResp = restTemplate.exchange(url("/admin/users"), HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminTok)), List.class);
        assertThat(adminResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // /users/me — Also OK for admin (admin is still a user)
        ResponseEntity<Map> meResp = restTemplate.exchange(url("/users/me"), HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminTok)), Map.class);
        assertThat(meResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
