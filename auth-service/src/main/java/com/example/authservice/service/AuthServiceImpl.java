package com.example.authservice.service;

import com.example.authservice.dto.ChangePasswordRequest;
import com.example.authservice.dto.ForgotPasswordRequest;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.LoginResponse;
import com.example.authservice.dto.RefreshRequest;
import com.example.authservice.dto.RegisterRequest;
import com.example.authservice.dto.ResetPasswordRequest;
import com.example.authservice.entity.PasswordResetToken;
import com.example.authservice.entity.RefreshToken;
import com.example.authservice.entity.Role;
import com.example.authservice.entity.User;
import com.example.authservice.exception.InvalidCredentialsException;
import com.example.authservice.exception.UserAlreadyExistsException;
import com.example.authservice.repository.PasswordResetTokenRepository;
import com.example.authservice.repository.RefreshTokenRepository;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.util.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditService auditService;

    @Value("${app.auth.refresh-expiry-days:7}")
    private int refreshExpiryDays;

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    auditService.log(null, "LOGIN_FAILED", resolveRequestIp());
                    return new InvalidCredentialsException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            auditService.log(user.getId(), "LOGIN_FAILED", resolveRequestIp());
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!user.isActive()) {
            auditService.log(user.getId(), "ACCOUNT_LOCKED", resolveRequestIp());
            throw new InvalidCredentialsException("Account is inactive");
        }

        auditService.log(user.getId(), "LOGIN_SUCCESS", resolveRequestIp());

        return issueTokens(user);
    }

    @Override
    public LoginResponse register(RegisterRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }

        Optional<User> existingUserByEmail = userRepository.findByEmail(request.getEmail());
        if (existingUserByEmail.isPresent()) {
            throw new UserAlreadyExistsException("User with this email already exists");
        }

        Optional<User> existingUserByUsername = userRepository.findByUsername(request.getUsername());
        if (existingUserByUsername.isPresent()) {
            throw new UserAlreadyExistsException("User with this username already exists");
        }

        User savedUser = userRepository.save(User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .active(true)
                .build());

        return issueTokens(savedUser);
    }

    @Override
    public LoginResponse refresh(RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(request.getRefreshToken())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new InvalidCredentialsException("Refresh token expired");
        }

        return issueTokens(refreshToken.getUser());
    }

    @Override
    public void logout(RefreshRequest request) {
        refreshTokenRepository.findByTokenAndRevokedFalse(request.getRefreshToken()).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Override
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = getUserByUsername(username);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Old password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        auditService.log(user.getId(), "PASSWORD_CHANGED", resolveRequestIp());
    }

    @Override
    public String forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("No user found for email"));

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);
        return resetToken.getToken();
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenAndUsedFalse(request.getToken())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid reset token"));

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new InvalidCredentialsException("Reset token expired");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        auditService.log(user.getId(), "PASSWORD_CHANGED", resolveRequestIp());

        token.setUsed(true);
        passwordResetTokenRepository.save(token);
    }

    @Override
    public boolean validateToken(String token) {
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

    @Override
    public String extractRole(String token) {
        return jwtTokenProvider.extractRole(token);
    }

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
    }

    private LoginResponse issueTokens(User user) {
        String accessToken = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole().name());

        String refreshTokenValue = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiryDate(LocalDateTime.now().plusDays(refreshExpiryDays))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return LoginResponse.builder()
                .token(accessToken)
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .username(user.getUsername())
                .userId(user.getId())
                .role(user.getRole())
                .build();
    }

    private String resolveRequestIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null && attributes.getRequest() != null) {
            return attributes.getRequest().getRemoteAddr();
        }
        return "unknown";
    }
}
