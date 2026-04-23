package com.example.authservice.config;

import com.example.authservice.entity.Role;
import com.example.authservice.entity.User;
import com.example.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrapConfig {

    @Bean
    public CommandLineRunner bootstrapAdminAndCustomer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.bootstrap.enabled}") boolean enabled,
            @Value("${app.admin.bootstrap.email}") String email,
            @Value("${app.admin.bootstrap.password}") String password,
            @Value("${app.admin.bootstrap.full-name}") String fullName,
            @Value("${app.customer.bootstrap.enabled}") boolean customerEnabled,
            @Value("${app.customer.bootstrap.email}") String customerEmail,
            @Value("${app.customer.bootstrap.password}") String customerPassword,
            @Value("${app.customer.bootstrap.full-name}") String customerFullName
    ) {
        return args -> {
            if (enabled && userRepository.findByEmail(email).isEmpty()) {
                if (password == null || password.isBlank()) {
                    throw new IllegalStateException("ADMIN_PASSWORD must be set when admin bootstrap is enabled");
                }
                userRepository.save(User.builder()
                        .email(email)
                        .fullName(fullName)
                        .passwordHash(passwordEncoder.encode(password))
                        .role(Role.ADMIN)
                        .active(true)
                        .build());
            }

            if (customerEnabled && userRepository.findByEmail(customerEmail).isEmpty()) {
                if (customerPassword == null || customerPassword.isBlank()) {
                    throw new IllegalStateException("CUSTOMER_PASSWORD must be set when customer bootstrap is enabled");
                }
                userRepository.save(User.builder()
                        .email(customerEmail)
                        .fullName(customerFullName)
                        .passwordHash(passwordEncoder.encode(customerPassword))
                        .role(Role.CUSTOMER)
                        .active(true)
                        .build());
            }
        };
    }
}
