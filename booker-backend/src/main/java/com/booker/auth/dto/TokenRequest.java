package com.booker.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Used by POST /auth/refresh and POST /auth/logout to receive the refresh token from the body. */
public record TokenRequest(@NotBlank String refreshToken) {}
