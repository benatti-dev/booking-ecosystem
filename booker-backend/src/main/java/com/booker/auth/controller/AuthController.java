package com.booker.auth.controller;

import com.booker.auth.dto.*;
import com.booker.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, token refresh, logout, password reset")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new client account — returns access + refresh tokens")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest req,
            HttpServletRequest httpReq) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(req, httpReq));
    }

    @PostMapping("/login")
    @Operation(summary = "Login — body: { username (email), password }")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest httpReq) {

        return ResponseEntity.ok(authService.login(req, httpReq));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token — body: { refreshToken }")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody TokenRequest req) {

        return ResponseEntity.ok(authService.refresh(req.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout — revokes the refresh token. Body: { refreshToken }")
    public ResponseEntity<Map<String, String>> logout(
            @Valid @RequestBody TokenRequest req,
            HttpServletRequest httpReq,
            @AuthenticationPrincipal UserDetails principal) {

        Long userId = principal instanceof com.booker.auth.entity.User u ? u.getId() : null;
        authService.logout(req.refreshToken(), httpReq, userId);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset email")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest req,
            HttpServletRequest httpReq) {

        authService.forgotPassword(req.email(), httpReq);
        return ResponseEntity.ok(Map.of("message",
                "If an account with this email exists, a reset link has been sent"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using the token from email")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest req,
            HttpServletRequest httpReq) {

        authService.resetPassword(req, httpReq);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully. Please log in."));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(authService.me(principal.getUsername()));
    }
}
