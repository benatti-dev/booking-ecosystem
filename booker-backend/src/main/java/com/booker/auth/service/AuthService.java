package com.booker.auth.service;

import com.booker.auth.dto.*;
import com.booker.auth.entity.User;
import com.booker.auth.entity.UserRole;
import com.booker.auth.repository.UserRepository;
import com.booker.shared.exception.BookerException;
import com.booker.shared.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Core authentication service.
 *
 * Token shape is aligned with @benatti/ng-auth-lib expectations:
 *   Request:  { username (=email), password }
 *   Response: { accessToken, refreshToken, tokenType, expiresIn, user }
 *
 * NOTE: JwtService import path (io.github.benatti.auth.service) is assumed —
 *       adjust to the actual package declared by benatti-auth-starter if it differs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;

    @Value("${app.refresh-token.expiry-days:7}")
    private int refreshExpiryDays;

    @Value("${app.jwt.expiration-ms:3600000}")
    private long jwtExpirationMs;

    // ── Register ──────────────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterRequest req, HttpServletRequest httpReq) {
        if (userRepository.existsByEmail(req.email())) {
            throw BookerException.conflict("An account with this email already exists");
        }

        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .phone(req.phone())
                .role(UserRole.CLIENT)
                .build();

        userRepository.save(user);

        auditLogService.log(AuditLogService.ACTION_REGISTER, user.getId(), httpReq,
                "{\"email\":\"" + user.getEmail() + "\"}");

        return issueTokenPair(user);
    }

    // ── Login ─────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest req, HttpServletRequest httpReq) {
        // username field holds the email — aligns with @benatti/ng-auth-lib contract
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password())
        );

        User user = userRepository.findByEmail(req.username())
                .orElseThrow(() -> BookerException.unauthorized("User not found"));

        auditLogService.log(AuditLogService.ACTION_LOGIN, user.getId(), httpReq);

        return issueTokenPair(user);
    }

    // ── Refresh ───────────────────────────────────────────────────

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        User user = refreshTokenService.validateRefreshToken(rawRefreshToken);

        // Token rotation: revoke old refresh token, issue a new pair
        refreshTokenService.revokeRefreshToken(rawRefreshToken);

        return issueTokenPair(user);
    }

    // ── Logout ────────────────────────────────────────────────────

    @Transactional
    public void logout(String rawRefreshToken, HttpServletRequest httpReq, Long userId) {
        refreshTokenService.revokeRefreshToken(rawRefreshToken);
        auditLogService.log(AuditLogService.ACTION_LOGOUT, userId, httpReq);
    }

    // ── Forgot Password ───────────────────────────────────────────

    @Transactional
    public void forgotPassword(String email, HttpServletRequest httpReq) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String rawToken = refreshTokenService.createPasswordResetToken(user);

            // Phase 1 STUB: log reset link instead of sending email.
            // Replace with real EmailService in Phase 4.
            log.info("[DEV STUB] Password reset link for {}: /reset-password?token={}",
                    user.getEmail(), rawToken);

            auditLogService.log(AuditLogService.ACTION_PASSWORD_RESET_REQUEST, user.getId(), httpReq);
        });
        // Always succeed to prevent email enumeration
    }

    // ── Reset Password ────────────────────────────────────────────

    @Transactional
    public void resetPassword(ResetPasswordRequest req, HttpServletRequest httpReq) {
        User user = refreshTokenService.validatePasswordResetToken(req.token());

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);

        // Invalidate all sessions after a password change
        refreshTokenService.revokeAllUserTokens(user.getId());
        refreshTokenService.consumePasswordResetToken(req.token());

        auditLogService.log(AuditLogService.ACTION_PASSWORD_RESET, user.getId(), httpReq);
    }

    // ── Current User ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserResponse me(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> BookerException.notFound("User not found"));
        return UserResponse.from(user);
    }

    // ── Private helpers ───────────────────────────────────────────

    private AuthResponse issueTokenPair(User user) {
        // JWT claims aligned with @benatti/ng-auth-lib TokenDecoderService expectations:
        // roles (array), email, username (= email used as login identifier)
        Map<String, Object> claims = Map.of(
                "roles", List.of("ROLE_" + user.getRole().name()),
                "role",  user.getRole().name(),
                "email", user.getEmail(),
                "username", user.getEmail(),
                "userId", user.getId(),
                "fullName", user.getFullName()
        );
        String accessToken = jwtService.generateToken(claims, user);
        String rawRefresh = refreshTokenService.createRefreshToken(user);

        return AuthResponse.of(accessToken, rawRefresh, jwtExpirationMs, UserResponse.from(user));
    }
}
