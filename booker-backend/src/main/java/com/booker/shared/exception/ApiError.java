package com.booker.shared.exception;

import java.time.Instant;
import java.util.List;

/**
 * Standard error response body returned by {@link GlobalExceptionHandler}.
 */
public record ApiError(
        int status,
        String error,
        String message,
        List<FieldError> fieldErrors,
        Instant timestamp
) {
    public record FieldError(String field, String message) {}

    public static ApiError of(int status, String error, String message) {
        return new ApiError(status, error, message, List.of(), Instant.now());
    }

    public static ApiError withFieldErrors(int status, String error, String message,
                                           List<FieldError> fieldErrors) {
        return new ApiError(status, error, message, fieldErrors, Instant.now());
    }
}
