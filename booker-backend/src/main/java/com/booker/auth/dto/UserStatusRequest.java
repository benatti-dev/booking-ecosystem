package com.booker.auth.dto;

import com.booker.auth.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

/** Request body for changing a user's account status. */
public record UserStatusRequest(
        @NotNull UserStatus status
) {}
