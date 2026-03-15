package com.example.authservice.config;

import com.example.authservice.entity.Role;
import com.example.authservice.entity.User;
import com.example.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
public class AdminBootstrapConfig {

    @Bean
    public CommandLineRunner bootstrapUsersAndCollections(
            UserRepository userRepository,
            MongoTemplate mongoTemplate,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.bootstrap.enabled:true}") boolean enabled,
            @Value("${app.admin.bootstrap.username:admin}") String username,
            @Value("${app.admin.bootstrap.email:admin@local.test}") String email,
            @Value("${app.admin.bootstrap.password:Admin@12345}") String password,
            @Value("${app.admin.bootstrap.full-name:System Admin}") String fullName,
            @Value("${app.customer.bootstrap.enabled:true}") boolean customerEnabled,
            @Value("${app.customer.bootstrap.username:customer}") String customerUsername,
            @Value("${app.customer.bootstrap.email:customer@local.test}") String customerEmail,
            @Value("${app.customer.bootstrap.password:Customer@12345}") String customerPassword,
            @Value("${app.customer.bootstrap.full-name:Default Customer}") String customerFullName
    ) {
        return args -> {
            List.of("users", "addresses", "refresh_tokens", "password_reset_tokens", "auth_logs")
                    .forEach(collectionName -> {
                        if (!mongoTemplate.collectionExists(collectionName)) {
                            mongoTemplate.createCollection(collectionName);
                        }
                    });

            if (enabled && userRepository.findByEmail(email).isEmpty() && userRepository.findByUsername(username).isEmpty()) {
                userRepository.save(User.builder()
                        .username(username)
                        .email(email)
                        .fullName(fullName)
                        .passwordHash(passwordEncoder.encode(password))
                        .role(Role.ADMIN)
                        .active(true)
                        .build());
            }

            if (customerEnabled && userRepository.findByEmail(customerEmail).isEmpty() && userRepository.findByUsername(customerUsername).isEmpty()) {
                userRepository.save(User.builder()
                        .username(customerUsername)
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
