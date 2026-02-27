package com.example.authservice.service;

import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.LoginResponse;
import com.example.authservice.dto.RegisterRequest;
import com.example.authservice.entity.User;
import com.example.authservice.exception.InvalidCredentialsException;
import com.example.authservice.exception.UserAlreadyExistsException;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.util.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Optional;

/**
 * AuthServiceImpl - Full implementation for authentication operations
 */
@Service
@Transactional
public class AuthServiceImpl implements AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse login(LoginRequest request) {
        logger.info("Login attempt for email: {}", request.getEmail());

        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            logger.warn("Invalid password attempt for user: {}", request.getEmail());
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Check if user is active
        if (!user.isActive()) {
            logger.warn("Inactive user login attempt: {}", request.getEmail());
            throw new InvalidCredentialsException("Account is inactive");
        }

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(
                user.getId(),
                user.getUsername(),
                Arrays.asList(user.getRole())
        );

        logger.info("Login successful for user: {}", user.getUsername());

        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .userId(user.getId())
                .build();
    }

    @Override
    public LoginResponse register(RegisterRequest request) {
        logger.info("Registration attempt for email: {}", request.getEmail());

        // Validate input
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }

        // Check if user already exists
        Optional<User> existingUserByEmail = userRepository.findByEmail(request.getEmail());
        if (existingUserByEmail.isPresent()) {
            logger.warn("Registration failed - email already exists: {}", request.getEmail());
            throw new UserAlreadyExistsException("User with this email already exists");
        }

        Optional<User> existingUserByUsername = userRepository.findByUsername(request.getUsername());
        if (existingUserByUsername.isPresent()) {
            logger.warn("Registration failed - username already exists: {}", request.getUsername());
            throw new UserAlreadyExistsException("User with this username already exists");
        }

        // Create new user
        User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .isActive(true)
                .build();

        // Save user to database
        User savedUser = userRepository.save(newUser);
        logger.info("User registered successfully: {}", savedUser.getUsername());

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(
                savedUser.getId(),
                savedUser.getUsername(),
                Arrays.asList(savedUser.getRole())
        );

        return LoginResponse.builder()
                .token(token)
                .username(savedUser.getUsername())
                .userId(savedUser.getId())
                .build();
    }

    @Override
    public boolean validateToken(String token) {
        logger.debug("Validating token");
        return jwtTokenProvider.validateToken(token);
    }

    @Override
    public String extractUserId(String token) {
        return jwtTokenProvider.extractUserId(token);
    }

    @Override
    public String extractUsername(String token) {
        return jwtTokenProvider.extractUsername(token);
    }
}
