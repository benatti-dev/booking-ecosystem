package com.booker.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEmployeeRequest(
        Long userId,
        Long branchId,
        @NotBlank @Size(max = 255) String displayName,
        String bio,
        String avatarUrl,
        @Size(max = 100) String position
) {}
