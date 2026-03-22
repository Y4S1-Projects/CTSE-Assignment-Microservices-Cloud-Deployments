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
 * Integration tests for UserController — /users/me, /users/profile, address CRUD.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserControllerIntegrationTest {

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

    /** Register a new unique customer and return their access token. */
    private LoginResponse registerCustomer() {
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        RegisterRequest req = RegisterRequest.builder()
                .email("cust_" + uid + "@test.com")
                .password("Password1!")
                .fullName("Customer " + uid)
                .build();
        ResponseEntity<LoginResponse> resp = restTemplate.postForEntity(url("/register"), req, LoginResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /users/me
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void getMe_authenticatedUser_returnsOwnProfile() {
        LoginResponse reg = registerCustomer();
        HttpEntity<Void> entity = new HttpEntity<>(bearerHeaders(reg.getAccessToken()));

        ResponseEntity<Map> resp = restTemplate.exchange(url("/users/me"), HttpMethod.GET, entity, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsKey("email");
        assertThat(resp.getBody()).containsKey("role");
        assertThat(resp.getBody().get("email")).isEqualTo(reg.getEmail());
    }

    @Test
    void getMe_unauthenticated_returns401Or403() {
        ResponseEntity<Map> resp = restTemplate.getForEntity(url("/users/me"), Map.class);
        assertThat(resp.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    void getMe_invalidToken_returns401Or403() {
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", "Bearer invalid.jwt.token");
        HttpEntity<Void> entity = new HttpEntity<>(h);

        ResponseEntity<Map> resp = restTemplate.exchange(url("/users/me"), HttpMethod.GET, entity, Map.class);
        assertThat(resp.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PUT /users/profile
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void updateProfile_validRequest_returnsUpdatedProfile() {
        LoginResponse reg = registerCustomer();
        UpdateProfileRequest req = UpdateProfileRequest.builder()
                .fullName("Updated Full Name").build();
        HttpEntity<UpdateProfileRequest> entity = new HttpEntity<>(req, bearerHeaders(reg.getAccessToken()));

        ResponseEntity<Map> resp = restTemplate.exchange(url("/users/profile"), HttpMethod.PUT, entity, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("fullName")).isEqualTo("Updated Full Name");
    }

    @Test
    void updateProfile_updateEmail_returnsNewEmail() {
        LoginResponse reg = registerCustomer();
        // Use unique new email to avoid conflicts
        String newEmail = "updated_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + "@new.test";
        UpdateProfileRequest req = UpdateProfileRequest.builder().email(newEmail).build();
        HttpEntity<UpdateProfileRequest> entity = new HttpEntity<>(req, bearerHeaders(reg.getAccessToken()));

        ResponseEntity<Map> resp = restTemplate.exchange(url("/users/profile"), HttpMethod.PUT, entity, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("email")).isEqualTo(newEmail);
    }

    @Test
    void updateProfile_unauthenticated_returns401Or403() {
        UpdateProfileRequest req = UpdateProfileRequest.builder().fullName("Nobody").build();
        ResponseEntity<Map> resp = restTemplate.exchange(url("/users/profile"), HttpMethod.PUT,
                new HttpEntity<>(req), Map.class);
        assertThat(resp.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Addresses embedded in /users/profile and /users/me
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void updateProfile_withSingleAddress_persistsInUserValues() {
        LoginResponse reg = registerCustomer();

        AddressRequest a1 = new AddressRequest();
        a1.setStreet("123 Main St");
        a1.setCity("Springfield");
        a1.setPostalCode("10001");
        a1.setDefault(true);

        UpdateProfileRequest req = UpdateProfileRequest.builder().addresses(List.of(a1)).build();
        ResponseEntity<Map> updateResp = restTemplate.exchange(
                url("/users/profile"),
                HttpMethod.PUT,
                new HttpEntity<>(req, bearerHeaders(reg.getAccessToken())),
                Map.class
        );

        assertThat(updateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> addresses = (List<Map<String, Object>>) updateResp.getBody().get("addresses");
        assertThat(addresses).hasSize(1);
        assertThat(addresses.get(0).get("street")).isEqualTo("123 Main St");
    }

    @Test
    void updateProfile_withThreeAddresses_succeeds() {
        LoginResponse reg = registerCustomer();

        AddressRequest a1 = new AddressRequest();
        a1.setStreet("A St");
        a1.setCity("A City");
        a1.setPostalCode("11111");
        a1.setDefault(true);

        AddressRequest a2 = new AddressRequest();
        a2.setStreet("B St");
        a2.setCity("B City");
        a2.setPostalCode("22222");
        a2.setDefault(false);

        AddressRequest a3 = new AddressRequest();
        a3.setStreet("C St");
        a3.setCity("C City");
        a3.setPostalCode("33333");
        a3.setDefault(false);

        UpdateProfileRequest req = UpdateProfileRequest.builder().addresses(List.of(a1, a2, a3)).build();
        ResponseEntity<Map> updateResp = restTemplate.exchange(
                url("/users/profile"),
                HttpMethod.PUT,
                new HttpEntity<>(req, bearerHeaders(reg.getAccessToken())),
                Map.class
        );

        assertThat(updateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> addresses = (List<Map<String, Object>>) updateResp.getBody().get("addresses");
        assertThat(addresses).hasSize(3);
    }

    @Test
    void updateProfile_withMoreThanThreeAddresses_returns400() {
        LoginResponse reg = registerCustomer();

        AddressRequest a1 = new AddressRequest();
        a1.setStreet("1");
        a1.setCity("C1");
        a1.setPostalCode("11111");
        a1.setDefault(true);

        AddressRequest a2 = new AddressRequest();
        a2.setStreet("2");
        a2.setCity("C2");
        a2.setPostalCode("22222");
        a2.setDefault(false);

        AddressRequest a3 = new AddressRequest();
        a3.setStreet("3");
        a3.setCity("C3");
        a3.setPostalCode("33333");
        a3.setDefault(false);

        AddressRequest a4 = new AddressRequest();
        a4.setStreet("4");
        a4.setCity("C4");
        a4.setPostalCode("44444");
        a4.setDefault(false);

        UpdateProfileRequest req = UpdateProfileRequest.builder().addresses(List.of(a1, a2, a3, a4)).build();
        ResponseEntity<Map> resp = restTemplate.exchange(
                url("/users/profile"),
                HttpMethod.PUT,
                new HttpEntity<>(req, bearerHeaders(reg.getAccessToken())),
                Map.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateProfile_withMultipleDefaults_returns400() {
        LoginResponse reg = registerCustomer();

        AddressRequest a1 = new AddressRequest();
        a1.setStreet("A St");
        a1.setCity("A City");
        a1.setPostalCode("11111");
        a1.setDefault(true);

        AddressRequest a2 = new AddressRequest();
        a2.setStreet("B St");
        a2.setCity("B City");
        a2.setPostalCode("22222");
        a2.setDefault(true);

        UpdateProfileRequest req = UpdateProfileRequest.builder().addresses(List.of(a1, a2)).build();
        ResponseEntity<Map> resp = restTemplate.exchange(
                url("/users/profile"),
                HttpMethod.PUT,
                new HttpEntity<>(req, bearerHeaders(reg.getAccessToken())),
                Map.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateProfile_replacesAddressesWhenPayloadChanges() {
        LoginResponse reg = registerCustomer();
        HttpHeaders headers = bearerHeaders(reg.getAccessToken());

        AddressRequest oldAddress = new AddressRequest();
        oldAddress.setStreet("Old St");
        oldAddress.setCity("Old City");
        oldAddress.setPostalCode("10000");
        oldAddress.setDefault(true);

        UpdateProfileRequest first = UpdateProfileRequest.builder().addresses(List.of(oldAddress)).build();
        ResponseEntity<Map> firstResp = restTemplate.exchange(url("/users/profile"), HttpMethod.PUT, new HttpEntity<>(first, headers), Map.class);
        List<Map<String, Object>> firstAddresses = (List<Map<String, Object>>) firstResp.getBody().get("addresses");
        assertThat(firstAddresses).hasSize(1);

        AddressRequest newAddress = new AddressRequest();
        newAddress.setStreet("New St");
        newAddress.setCity("New City");
        newAddress.setPostalCode("20000");
        newAddress.setDefault(true);

        UpdateProfileRequest second = UpdateProfileRequest.builder().addresses(List.of(newAddress)).build();
        ResponseEntity<Map> secondResp = restTemplate.exchange(url("/users/profile"), HttpMethod.PUT, new HttpEntity<>(second, headers), Map.class);
        List<Map<String, Object>> secondAddresses = (List<Map<String, Object>>) secondResp.getBody().get("addresses");

        assertThat(secondAddresses).hasSize(1);
        assertThat(secondAddresses.get(0).get("street")).isEqualTo("New St");
    }

    @Test
    void getMe_returnsAddressesEmbeddedInProfile() {
        LoginResponse reg = registerCustomer();
        HttpHeaders headers = bearerHeaders(reg.getAccessToken());

        AddressRequest a1 = new AddressRequest();
        a1.setStreet("Embedded St");
        a1.setCity("Embedded City");
        a1.setPostalCode("90909");
        a1.setDefault(true);

        UpdateProfileRequest req = UpdateProfileRequest.builder().addresses(List.of(a1)).build();
        restTemplate.exchange(url("/users/profile"), HttpMethod.PUT, new HttpEntity<>(req, headers), Map.class);

        ResponseEntity<Map> meResp = restTemplate.exchange(url("/users/me"), HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat(meResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<Map<String, Object>> addresses = (List<Map<String, Object>>) meResp.getBody().get("addresses");
        assertThat(addresses).hasSize(1);
        assertThat(addresses.get(0).get("street")).isEqualTo("Embedded St");
        assertThat(meResp.getBody()).containsKey("primaryAddress");
    }
}
