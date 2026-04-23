package com.example.apigateway;

import com.example.apigateway.filter.JwtAuthenticationFilter;
import com.example.apigateway.filter.LoggingFilter;
import com.example.apigateway.filter.RateLimitFilter;
import com.example.apigateway.util.JwtTokenValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiGatewayApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenValidator jwtTokenValidator;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private LoggingFilter loggingFilter;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    private WebTestClient webTestClient;

    @org.junit.jupiter.api.BeforeEach
    void setUpClient() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void contextLoads() {
        assertThat(jwtTokenValidator).isNotNull();
        assertThat(jwtAuthenticationFilter).isNotNull();
        assertThat(loggingFilter).isNotNull();
        assertThat(rateLimitFilter).isNotNull();
    }

    @Test
    void filterOrdering_loggingBeforeRateLimitBeforeJwt() {
        // Logging = -3, RateLimit = -2, JWT = -1
        assertThat(loggingFilter.getOrder()).isLessThan(rateLimitFilter.getOrder());
        assertThat(rateLimitFilter.getOrder()).isLessThan(jwtAuthenticationFilter.getOrder());
    }

    @Test
    void corsPreflight_onProtectedRoute_returnsAllowOriginHeader() {
        webTestClient.options()
                .uri("/auth/admin/users")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectHeader().exists("Access-Control-Allow-Origin")
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:3000");
    }
}
