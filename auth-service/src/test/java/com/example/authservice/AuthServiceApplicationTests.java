package com.example.authservice;

import com.example.authservice.repository.*;
import com.example.authservice.service.AuthService;
import com.example.authservice.util.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceApplicationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private AuthLogRepository authLogRepository;

    @Test
    void contextLoads() {
        // All required beans must be present
        assertThat(authService).isNotNull();
        assertThat(jwtTokenProvider).isNotNull();
        assertThat(userRepository).isNotNull();
        assertThat(refreshTokenRepository).isNotNull();
        assertThat(passwordResetTokenRepository).isNotNull();
        assertThat(addressRepository).isNotNull();
        assertThat(authLogRepository).isNotNull();
    }

    @Test
    void bootstrapAdmin_isCreatedOnStartup() {
        // The AdminBootstrapConfig seeds admin@local.test on first run
        assertThat(userRepository.findByEmail("admin@local.test")).isPresent();
    }
}

