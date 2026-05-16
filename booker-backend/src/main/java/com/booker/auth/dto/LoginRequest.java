package com.booker.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Login request DTO.
 * Field name is "username" to match the @benatti/ng-auth-lib AuthService.login() contract,
 * but we validate it as an email address.
 */
public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String username,   // the library sends { username, password }

        @NotBlank(message = "Password is required")
        String password
) {}
