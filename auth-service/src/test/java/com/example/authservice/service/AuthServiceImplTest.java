package com.example.authservice.service;

import com.example.authservice.dto.*;
import com.example.authservice.entity.*;
import com.example.authservice.exception.InvalidCredentialsException;
import com.example.authservice.exception.UserAlreadyExistsException;
import com.example.authservice.repository.*;
import com.example.authservice.util.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService auditService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User activeUser;

    @BeforeEach
    void setUpUser() {
        activeUser = User.builder()
                .id("user-123")
                .email("alice@example.com")
                .passwordHash("hashed_pw")
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        // Default stub: generateToken always returns a predictable value
        lenient().when(jwtTokenProvider.generateToken(any(), any(), any())).thenReturn("mock_access_token");
        lenient().when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // login()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void login_validCredentials_returnsLoginResponse() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("secret", "hashed_pw")).thenReturn(true);

        LoginResponse resp = authService.login(
                LoginRequest.builder().email("alice@example.com").password("secret").build());

        assertThat(resp).isNotNull();
        assertThat(resp.getAccessToken()).isEqualTo("mock_access_token");
                assertThat(resp.getEmail()).isEqualTo("alice@example.com");
        verify(auditService).log(eq("user-123"), eq("LOGIN_SUCCESS"), any());
    }

    @Test
    void login_userNotFound_throwsInvalidCredentials_andLogsFailure() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.login(LoginRequest.builder().email("nobody@example.com").password("x").build()))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(auditService).log(isNull(), eq("LOGIN_FAILED"), any());
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials_andLogsFailure() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrong", "hashed_pw")).thenReturn(false);

        assertThatThrownBy(() ->
                authService.login(LoginRequest.builder().email("alice@example.com").password("wrong").build()))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(auditService).log(eq("user-123"), eq("LOGIN_FAILED"), any());
    }

    @Test
    void login_inactiveUser_throwsInvalidCredentials_andLogsAccountLocked() {
        activeUser.setActive(false);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("secret", "hashed_pw")).thenReturn(true);

        assertThatThrownBy(() ->
                authService.login(LoginRequest.builder().email("alice@example.com").password("secret").build()))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("inactive");
        verify(auditService).log(eq("user-123"), eq("ACCOUNT_LOCKED"), any());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // register()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void register_newUser_savesUserAndReturnsTokens() {
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password1")).thenReturn("hashed_pw1");
        User saved = User.builder().id("user-456").email("bob@example.com")
                .role(Role.CUSTOMER).active(true).build();
        when(userRepository.save(any(User.class))).thenReturn(saved);

        LoginResponse resp = authService.register(
                RegisterRequest.builder().email("bob@example.com")
                        .password("password1").fullName("Bob Smith").build());

        assertThat(resp).isNotNull();
        assertThat(resp.getEmail()).isEqualTo("bob@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsUserAlreadyExists() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() ->
                authService.register(RegisterRequest.builder()
                        .email("alice@example.com").password("password1").fullName("New").build()))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("email");
    }

    @Test
    void register_nullEmail_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                authService.register(RegisterRequest.builder()
                        .email(null).password("password1").fullName("X").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email is required");
    }

    @Test
    void register_shortPassword_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                authService.register(RegisterRequest.builder()
                        .email("x@test.com").password("abc").fullName("X").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 6 characters");
    }

    @Test
    void register_blankEmail_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                authService.register(RegisterRequest.builder()
                        .email("   ").password("password1").fullName("X").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email is required");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // refresh()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void refresh_validToken_returnsNewTokens() {
        RefreshToken rt = RefreshToken.builder()
                .token("valid_rt")
                .userId(activeUser.getId())
                .expiryDate(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenAndRevokedFalse("valid_rt")).thenReturn(Optional.of(rt));
        when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));

        LoginResponse resp = authService.refresh(RefreshRequest.builder().refreshToken("valid_rt").build());

        assertThat(resp.getAccessToken()).isEqualTo("mock_access_token");
    }

    @Test
    void refresh_tokenNotFound_throwsInvalidCredentials() {
        when(refreshTokenRepository.findByTokenAndRevokedFalse("bad_rt")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.refresh(RefreshRequest.builder().refreshToken("bad_rt").build()))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void refresh_expiredToken_revokesAndThrowsInvalidCredentials() {
        RefreshToken expired = RefreshToken.builder()
                .token("exp_rt")
                .userId(activeUser.getId())
                .expiryDate(LocalDateTime.now().minusHours(1))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenAndRevokedFalse("exp_rt")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() ->
                authService.refresh(RefreshRequest.builder().refreshToken("exp_rt").build()))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("expired");

        verify(refreshTokenRepository).save(argThat(RefreshToken::isRevoked));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // logout()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void logout_validToken_revokesToken() {
        RefreshToken rt = RefreshToken.builder().token("rt").userId(activeUser.getId()).revoked(false).build();
        when(refreshTokenRepository.findByTokenAndRevokedFalse("rt")).thenReturn(Optional.of(rt));

        authService.logout(RefreshRequest.builder().refreshToken("rt").build());

        verify(refreshTokenRepository).save(argThat(RefreshToken::isRevoked));
    }

    @Test
    void logout_tokenAlreadyRevoked_doesNotThrow() {
        when(refreshTokenRepository.findByTokenAndRevokedFalse("gone")).thenReturn(Optional.empty());

        assertThatCode(() ->
                authService.logout(RefreshRequest.builder().refreshToken("gone").build()))
                .doesNotThrowAnyException();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // changePassword()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void changePassword_correctOldPassword_savesNewHash() {
                when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("old_pw", "hashed_pw")).thenReturn(true);
        when(passwordEncoder.encode("new_pw")).thenReturn("new_hashed");
        when(userRepository.save(any())).thenReturn(activeUser);

                authService.changePassword("alice@example.com",
                ChangePasswordRequest.builder().oldPassword("old_pw").newPassword("new_pw").build());

        verify(userRepository).save(argThat(u -> "new_hashed".equals(u.getPasswordHash())));
        verify(auditService).log(eq("user-123"), eq("PASSWORD_CHANGED"), any());
    }

    @Test
    void changePassword_wrongOldPassword_throwsInvalidCredentials() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("bad_pw", "hashed_pw")).thenReturn(false);

        assertThatThrownBy(() ->
                authService.changePassword("alice@example.com",
                        ChangePasswordRequest.builder().oldPassword("bad_pw").newPassword("new_pw").build()))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Old password is incorrect");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // forgotPassword() / resetPassword()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void forgotPassword_existingEmail_savesTokenAndReturnsIt() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String token = authService.forgotPassword(
                ForgotPasswordRequest.builder().email("alice@example.com").build());

        assertThat(token).isNotNull().isNotBlank();
        verify(passwordResetTokenRepository).save(any());
    }

    @Test
    void forgotPassword_unknownEmail_throwsInvalidCredentials() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.forgotPassword(ForgotPasswordRequest.builder().email("ghost@example.com").build()))
                .isInstanceOf(InvalidCredentialsException.class);
    }
    @Test
    void resetPassword_validToken_changesPassword() {
        PasswordResetToken prt = PasswordResetToken.builder()
                .token(hashToken("rt-abc"))
                .userId(activeUser.getId())
                .expiryDate(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();
        when(passwordResetTokenRepository.findByTokenAndUsedFalse(hashToken("rt-abc"))).thenReturn(Optional.of(prt));
        when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.encode("new_pw")).thenReturn("new_hashed");
        when(userRepository.save(any())).thenReturn(activeUser);
        when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.resetPassword(
                ResetPasswordRequest.builder().token("rt-abc").newPassword("new_pw").build());

        verify(userRepository).save(argThat(u -> "new_hashed".equals(u.getPasswordHash())));
        verify(passwordResetTokenRepository).save(argThat(PasswordResetToken::isUsed));
        verify(auditService).log(eq("user-123"), eq("PASSWORD_CHANGED"), any());
    }

    @Test
    void resetPassword_invalidToken_throwsInvalidCredentials() {
        when(passwordResetTokenRepository.findByTokenAndUsedFalse(hashToken("bad"))).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.resetPassword(
                        ResetPasswordRequest.builder().token("bad").newPassword("pw").build()))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid reset token");
    }

    @Test
    void resetPassword_expiredToken_throwsInvalidCredentials() {
        PasswordResetToken expired = PasswordResetToken.builder()
                .token(hashToken("exp-rt"))
                .userId(activeUser.getId())
                .expiryDate(LocalDateTime.now().minusMinutes(1))
                .used(false)
                .build();
        when(passwordResetTokenRepository.findByTokenAndUsedFalse(hashToken("exp-rt"))).thenReturn(Optional.of(expired));

        assertThatThrownBy(() ->
                authService.resetPassword(
                        ResetPasswordRequest.builder().token("exp-rt").newPassword("pw").build()))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("expired");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // validateToken / extractors
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void validateToken_delegatesToJwtProvider() {
        when(jwtTokenProvider.validateToken("tok")).thenReturn(true);
        assertThat(authService.validateToken("tok")).isTrue();
    }

    @Test
        void extractEmail_delegatesToJwtProvider() {
                when(jwtTokenProvider.extractEmail("tok")).thenReturn("alice@example.com");
                assertThat(authService.extractEmail("tok")).isEqualTo("alice@example.com");
    }

    @Test
    void extractRole_delegatesToJwtProvider() {
        when(jwtTokenProvider.extractRole("tok")).thenReturn("CUSTOMER");
        assertThat(authService.extractRole("tok")).isEqualTo("CUSTOMER");
    }

    @Test
        void getUserByEmail_existingUser_returnsUser() {
                when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(activeUser));
                User result = authService.getUserByEmail("alice@example.com");
                assertThat(result.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
        void getUserByEmail_unknownUser_throwsInvalidCredentials() {
                when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
                assertThatThrownBy(() -> authService.getUserByEmail("ghost@example.com"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
        private String hashToken(String value) {
                try {
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
                } catch (NoSuchAlgorithmException ex) {
                        throw new RuntimeException(ex);
                }
        }
}
