package com.booker.auth;

import com.booker.auth.dto.LoginRequest;
import com.booker.auth.dto.RegisterRequest;
import com.booker.auth.entity.User;
import com.booker.auth.entity.UserRole;
import com.booker.auth.entity.UserStatus;
import com.booker.auth.repository.UserRepository;
import com.booker.auth.service.AuditLogService;
import com.booker.auth.service.AuthService;
import com.booker.auth.service.RefreshTokenService;
import com.booker.shared.exception.BookerException;
import com.booker.shared.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Unit Tests")
class AuthServiceUnitTest {

    @Mock UserRepository       userRepository;
    @Mock PasswordEncoder      passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService           jwtService;
    @Mock RefreshTokenService  refreshTokenService;
    @Mock AuditLogService      auditLogService;

    @InjectMocks AuthService authService;

    private static final String CLIENT_IP = "127.0.0.1";

    private User stubUser;

    @BeforeEach
    void setUp() {
        stubUser = User.builder()
                .id(1L)
                .email("user@test.com")
                .fullName("Test User")
                .role(UserRole.CLIENT)
                .status(UserStatus.ACTIVE)
                .passwordHash("$2a$10$hashed")
                .build();
    }

    // ── Register ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("new email → user saved, tokens issued")
        void register_success() {
            RegisterRequest req = new RegisterRequest("user@test.com", "secret123", "Test User", "+380501234567");
            when(userRepository.existsByEmail("user@test.com")).thenReturn(false);
            when(passwordEncoder.encode("secret123")).thenReturn("$2a$10$hashed");
            when(userRepository.save(any(User.class))).thenReturn(stubUser);
            when(jwtService.generateToken(anyMap(), any())).thenReturn("access-token");
            when(refreshTokenService.createRefreshToken(any())).thenReturn("refresh-token");

            var response = authService.register(req, CLIENT_IP);

            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
            verify(userRepository).save(any(User.class));
            verify(auditLogService).log(eq(AuditLogService.ACTION_REGISTER), eq(1L), eq(CLIENT_IP), anyString());
        }

        @Test
        @DisplayName("duplicate email → throws 409 CONFLICT")
        void register_duplicateEmail_throws() {
            RegisterRequest req = new RegisterRequest("user@test.com", "secret123", "Test User", null);
            when(userRepository.existsByEmail("user@test.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req, CLIENT_IP))
                    .isInstanceOf(BookerException.class)
                    .hasMessageContaining("email already exists");

            verify(userRepository, never()).save(any());
        }
    }

    // ── Login ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("correct credentials → tokens issued")
        void login_success() {
            LoginRequest req = new LoginRequest("user@test.com", "secret123");
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(stubUser));
            when(jwtService.generateToken(anyMap(), any())).thenReturn("access-token");
            when(refreshTokenService.createRefreshToken(any())).thenReturn("refresh-token");

            var response = authService.login(req, CLIENT_IP);

            assertThat(response.accessToken()).isEqualTo("access-token");
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(auditLogService).log(eq(AuditLogService.ACTION_LOGIN), eq(1L), eq(CLIENT_IP));
        }

        @Test
        @DisplayName("wrong password → AuthenticationManager throws, propagated to caller")
        void login_badCredentials_throws() {
            LoginRequest req = new LoginRequest("user@test.com", "wrong");
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(req, CLIENT_IP))
                    .isInstanceOf(BadCredentialsException.class);

            verify(userRepository, never()).findByEmail(anyString());
        }
    }

    // ── Refresh ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("valid refresh token → new token pair issued")
        void refresh_success() {
            when(refreshTokenService.validateAndRevokeRefreshToken("raw-refresh")).thenReturn(stubUser);
            when(jwtService.generateToken(anyMap(), any())).thenReturn("new-access-token");
            when(refreshTokenService.createRefreshToken(any())).thenReturn("new-refresh-token");

            var response = authService.refresh("raw-refresh");

            assertThat(response.accessToken()).isEqualTo("new-access-token");
            assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        }

        @Test
        @DisplayName("expired refresh token → BookerException propagated from RefreshTokenService")
        void refresh_expiredToken_throws() {
            when(refreshTokenService.validateAndRevokeRefreshToken("bad-token"))
                    .thenThrow(BookerException.unauthorized("Refresh token expired or revoked"));

            assertThatThrownBy(() -> authService.refresh("bad-token"))
                    .isInstanceOf(BookerException.class)
                    .hasMessageContaining("Refresh token");
        }
    }

    // ── Logout ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("logout revokes refresh token and writes audit log")
        void logout_success() {
            authService.logout("refresh-token", CLIENT_IP, 1L);

            verify(refreshTokenService).revokeRefreshToken("refresh-token");
            verify(auditLogService).log(eq(AuditLogService.ACTION_LOGOUT), eq(1L), eq(CLIENT_IP));
        }
    }
}
