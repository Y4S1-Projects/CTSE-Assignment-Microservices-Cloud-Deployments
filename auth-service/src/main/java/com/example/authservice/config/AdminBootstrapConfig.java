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
            @Value("${app.admin.bootstrap.enabled:true}") boolean enabled,
            @Value("${app.admin.bootstrap.email:admin@local.test}") String email,
            @Value("${app.admin.bootstrap.password:Admin@12345}") String password,
            @Value("${app.admin.bootstrap.full-name:System Admin}") String fullName,
            @Value("${app.customer.bootstrap.enabled:true}") boolean customerEnabled,
            @Value("${app.customer.bootstrap.email:customer@local.test}") String customerEmail,
            @Value("${app.customer.bootstrap.password:Customer@12345}") String customerPassword,
            @Value("${app.customer.bootstrap.full-name:Default Customer}") String customerFullName
    ) {
        return args -> {
            if (enabled && userRepository.findByEmail(email).isEmpty()) {
                userRepository.save(User.builder()
                        .email(email)
                        .fullName(fullName)
                        .passwordHash(passwordEncoder.encode(password))
                        .role(Role.ADMIN)
                        .active(true)
                        .build());
            }

            if (customerEnabled && userRepository.findByEmail(customerEmail).isEmpty()) {
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
