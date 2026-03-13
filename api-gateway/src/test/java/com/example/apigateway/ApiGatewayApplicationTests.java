package com.example.apigateway;

import com.example.apigateway.filter.JwtAuthenticationFilter;
import com.example.apigateway.filter.LoggingFilter;
import com.example.apigateway.filter.RateLimitFilter;
import com.example.apigateway.util.JwtTokenValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiGatewayApplicationTests {

    @Autowired
    private JwtTokenValidator jwtTokenValidator;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private LoggingFilter loggingFilter;

    @Autowired
    private RateLimitFilter rateLimitFilter;

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
}
