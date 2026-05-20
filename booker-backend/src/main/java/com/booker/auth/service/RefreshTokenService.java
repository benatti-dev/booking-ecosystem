package com.booker.auth.service;

import com.booker.auth.entity.PasswordResetToken;
import com.booker.auth.entity.RefreshToken;
import com.booker.auth.entity.User;
import com.booker.auth.repository.PasswordResetTokenRepository;
import com.booker.auth.repository.RefreshTokenRepository;
import com.booker.shared.exception.BookerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${app.refresh-token.expiry-days:7}")
    private int refreshExpiryDays;

    @Value("${app.password-reset.expiry-hours:1}")
    private int resetExpiryHours;

    // ── Refresh Token ──────────────────────────────────────────────────────

    /**
     * Creates a new refresh token for the given user.
     * @return raw UUID token to be sent to the client (NOT stored in DB)
     */
    @Transactional
    public String createRefreshToken(User user) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = sha256Hex(rawToken);

        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(refreshExpiryDays, ChronoUnit.DAYS))
                .build();

        refreshTokenRepository.save(entity);
        return rawToken;
    }

    /**
     * Atomically validates and revokes the refresh token within a single
     * pessimistic-write-locked transaction.  This prevents two concurrent
     * requests from replaying the same token and both receiving a new
     * access token (token-replay attack).
     *
     * @return the {@link User} that owned the token
     */
    @Transactional
    public User validateAndRevokeRefreshToken(String rawToken) {
        String tokenHash = sha256Hex(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> BookerException.unauthorized("Invalid refresh token"));

        if (!token.isValid()) {
            throw BookerException.unauthorized("Refresh token is expired or revoked");
        }
        token.setRevoked(true);
        refreshTokenRepository.save(token);
        return token.getUser();
    }

    /**
     * Validates the raw refresh token and returns the associated user.
     * For read-only contexts only (e.g., token introspection).
     * For the actual token-rotation flow use {@link #validateAndRevokeRefreshToken}.
     */
    @Transactional(readOnly = true)
    public User validateRefreshToken(String rawToken) {
        String tokenHash = sha256Hex(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> BookerException.unauthorized("Invalid refresh token"));

        if (!token.isValid()) {
            throw BookerException.unauthorized("Refresh token is expired or revoked");
        }
        return token.getUser();
    }

    /**
     * Revokes a single refresh token (logout).
     */
    @Transactional
    public void revokeRefreshToken(String rawToken) {
        String tokenHash = sha256Hex(rawToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(t -> {
                    t.setRevoked(true);
                    refreshTokenRepository.save(t);
                });
    }

    /**
     * Revokes all refresh tokens for a user (e.g., password change, forced logout).
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    // ── Password Reset Token ──────────────────────────────────────────────

    /**
     * Creates a password reset token.
     * @return raw UUID token to be sent via email
     */
    @Transactional
    public String createPasswordResetToken(User user) {
        // Invalidate existing tokens for this user
        passwordResetTokenRepository.invalidateAllByUserId(user.getId());

        String rawToken = UUID.randomUUID().toString();
        String tokenHash = sha256Hex(rawToken);

        PasswordResetToken entity = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(resetExpiryHours, ChronoUnit.HOURS))
                .build();

        passwordResetTokenRepository.save(entity);
        return rawToken;
    }

    /**
     * Validates reset token and returns the associated user.
     */
    @Transactional(readOnly = true)
    public User validatePasswordResetToken(String rawToken) {
        String tokenHash = sha256Hex(rawToken);
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> BookerException.badRequest("Invalid or expired reset token"));

        if (!token.isValid()) {
            throw BookerException.badRequest("Reset token has expired or already been used");
        }
        return token.getUser();
    }

    /**
     * Marks reset token as used.
     */
    @Transactional
    public void consumePasswordResetToken(String rawToken) {
        String tokenHash = sha256Hex(rawToken);
        passwordResetTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(t -> {
                    t.setUsed(true);
                    passwordResetTokenRepository.save(t);
                });
    }

    // ── Cleanup Scheduler ─────────────────────────────────────────────────

    /** Runs daily at 3:00 AM to delete stale tokens. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        Instant now = Instant.now();
        int rt  = refreshTokenRepository.deleteExpiredAndRevoked(now);
        int prt = passwordResetTokenRepository.deleteExpiredAndUsed(now);
        if (rt > 0 || prt > 0) {
            log.info("[TokenCleanup] Deleted {} refresh tokens, {} reset tokens", rt, prt);
        }
    }

    // ── Utility ─────────────────────────────────────────────────────────────

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
