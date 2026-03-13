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
                .username("cust_" + uid)
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
        assertThat(resp.getBody()).containsKey("username");
        assertThat(resp.getBody()).containsKey("email");
        assertThat(resp.getBody()).containsKey("role");
        assertThat(resp.getBody().get("username")).isEqualTo(reg.getUsername());
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
    // POST /users/addresses
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void addAddress_validRequest_returns201WithAddressData() {
        LoginResponse reg = registerCustomer();
        AddressRequest req = new AddressRequest();
        req.setStreet("123 Main St");
        req.setCity("Springfield");
        req.setPostalCode("10001");
        req.setDefault(true);
        HttpEntity<AddressRequest> entity = new HttpEntity<>(req, bearerHeaders(reg.getAccessToken()));

        ResponseEntity<Map> resp = restTemplate.exchange(url("/users/addresses"), HttpMethod.POST, entity, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().get("street")).isEqualTo("123 Main St");
        assertThat(resp.getBody().get("city")).isEqualTo("Springfield");
        assertThat(resp.getBody().get("postalCode")).isEqualTo("10001");
        assertThat(resp.getBody()).containsKey("id");
    }

    @Test
    void addAddress_unauthenticated_returns401Or403() {
        AddressRequest req = new AddressRequest();
        req.setStreet("1 Test");
        req.setCity("City");
        req.setPostalCode("00000");
        req.setDefault(false);
        ResponseEntity<Map> resp = restTemplate.exchange(url("/users/addresses"), HttpMethod.POST,
                new HttpEntity<>(req), Map.class);
        assertThat(resp.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /users/addresses
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void getAddresses_noAddresses_returnsEmptyList() {
        LoginResponse reg = registerCustomer();
        HttpEntity<Void> entity = new HttpEntity<>(bearerHeaders(reg.getAccessToken()));

        ResponseEntity<List> resp = restTemplate.exchange(url("/users/addresses"), HttpMethod.GET, entity, List.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEmpty();
    }

    @Test
    void getAddresses_afterAddingTwo_returnsTwoAddresses() {
        LoginResponse reg = registerCustomer();
        HttpHeaders h = bearerHeaders(reg.getAccessToken());

        // Add first address
        AddressRequest req1 = new AddressRequest();
        req1.setStreet("A St");
        req1.setCity("CityA");
        req1.setPostalCode("11111");
        req1.setDefault(true);
        restTemplate.exchange(url("/users/addresses"), HttpMethod.POST,
            new HttpEntity<>(req1, h), Map.class);
        // Add second address
        AddressRequest req2 = new AddressRequest();
        req2.setStreet("B St");
        req2.setCity("CityB");
        req2.setPostalCode("22222");
        req2.setDefault(false);
        restTemplate.exchange(url("/users/addresses"), HttpMethod.POST,
            new HttpEntity<>(req2, h), Map.class);

        ResponseEntity<List> resp = restTemplate.exchange(url("/users/addresses"), HttpMethod.GET,
                new HttpEntity<>(h), List.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(2);
    }

    @Test
    void getAddresses_unauthenticated_returns401Or403() {
        ResponseEntity<List> resp = restTemplate.exchange(url("/users/addresses"), HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()), List.class);
        assertThat(resp.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PUT /users/addresses/{id}
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void updateAddress_ownAddress_returns200WithUpdatedData() {
        LoginResponse reg = registerCustomer();
        HttpHeaders h = bearerHeaders(reg.getAccessToken());

        // Create an address
        AddressRequest createReq = new AddressRequest();
        createReq.setStreet("Old St");
        createReq.setCity("Old City");
        createReq.setPostalCode("00000");
        createReq.setDefault(false);
        ResponseEntity<Map> created = restTemplate.exchange(url("/users/addresses"), HttpMethod.POST,
            new HttpEntity<>(createReq, h), Map.class);
        String addressId = (String) created.getBody().get("id");

        // Update it
        AddressRequest update = new AddressRequest();
        update.setStreet("New St");
        update.setCity("New City");
        update.setPostalCode("99999");
        update.setDefault(false);
        ResponseEntity<Map> resp = restTemplate.exchange(url("/users/addresses/" + addressId), HttpMethod.PUT,
                new HttpEntity<>(update, h), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("street")).isEqualTo("New St");
        assertThat(resp.getBody().get("city")).isEqualTo("New City");
    }

    @Test
    void updateAddress_otherUsersAddress_returns400() {
        LoginResponse user1 = registerCustomer();
        LoginResponse user2 = registerCustomer();

        // User1 creates an address
        AddressRequest createReq = new AddressRequest();
        createReq.setStreet("U1 St");
        createReq.setCity("City");
        createReq.setPostalCode("11111");
        createReq.setDefault(false);
        ResponseEntity<Map> created = restTemplate.exchange(url("/users/addresses"), HttpMethod.POST,
            new HttpEntity<>(createReq,
                        bearerHeaders(user1.getAccessToken())), Map.class);
        String addressId = (String) created.getBody().get("id");

        // User2 tries to update user1's address
        AddressRequest update = new AddressRequest();
        update.setStreet("Hacked");
        update.setCity("Evil");
        update.setPostalCode("00000");
        update.setDefault(false);
        ResponseEntity<Map> resp = restTemplate.exchange(url("/users/addresses/" + addressId), HttpMethod.PUT,
                new HttpEntity<>(update, bearerHeaders(user2.getAccessToken())), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST); // IllegalArgumentException → 400
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DELETE /users/addresses/{id}
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void deleteAddress_ownAddress_returns204() {
        LoginResponse reg = registerCustomer();
        HttpHeaders h = bearerHeaders(reg.getAccessToken());

        AddressRequest createReq = new AddressRequest();
        createReq.setStreet("Del St");
        createReq.setCity("City");
        createReq.setPostalCode("55555");
        createReq.setDefault(false);
        ResponseEntity<Map> created = restTemplate.exchange(url("/users/addresses"), HttpMethod.POST,
            new HttpEntity<>(createReq, h), Map.class);
        String addressId = (String) created.getBody().get("id");

        ResponseEntity<Void> respDel = restTemplate.exchange(url("/users/addresses/" + addressId),
                HttpMethod.DELETE, new HttpEntity<>(h), Void.class);

        assertThat(respDel.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void deleteAddress_notFound_returns400() {
        LoginResponse reg = registerCustomer();
        ResponseEntity<Map> resp = restTemplate.exchange(
                url("/users/addresses/00000000-0000-0000-0000-000000000000"),
                HttpMethod.DELETE, new HttpEntity<>(bearerHeaders(reg.getAccessToken())), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void deleteAddress_afterDeletion_notReturnedInList() {
        LoginResponse reg = registerCustomer();
        HttpHeaders h = bearerHeaders(reg.getAccessToken());

        AddressRequest createReq = new AddressRequest();
        createReq.setStreet("ToDelete");
        createReq.setCity("City");
        createReq.setPostalCode("12345");
        createReq.setDefault(false);
        ResponseEntity<Map> created = restTemplate.exchange(url("/users/addresses"), HttpMethod.POST,
            new HttpEntity<>(createReq, h), Map.class);
        String addressId = (String) created.getBody().get("id");

        restTemplate.exchange(url("/users/addresses/" + addressId), HttpMethod.DELETE, new HttpEntity<>(h), Void.class);

        ResponseEntity<List> listResp = restTemplate.exchange(url("/users/addresses"), HttpMethod.GET, new HttpEntity<>(h), List.class);
        assertThat(listResp.getBody()).noneMatch(a -> addressId.equals(((Map) a).get("id")));
    }
}
