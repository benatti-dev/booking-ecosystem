package com.booker.auth.dto;

/**
 * Returned on login and token refresh.
 * Shape matches what @benatti/ng-auth-lib's AuthService.handleAuthResponse() expects:
 *   { accessToken, refreshToken, user }
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
    public static AuthResponse of(String accessToken, String refreshToken,
                                  long expiresInMs, UserResponse user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInMs, user);
    }
}
