package com.booker.business.dto;

import java.time.Instant;

public record EmployeeResponse(
        Long id,
        Long userId,
        Long businessId,
        Long branchId,
        String displayName,
        String bio,
        String avatarUrl,
        String position,
        boolean isActive,
        Instant createdAt
) {}
