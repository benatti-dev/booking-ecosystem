package com.booker.business.dto;

import java.time.Instant;

public record BranchResponse(
        Long id,
        Long businessId,
        String name,
        String address,
        String city,
        String country,
        String postalCode,
        Double latitude,
        Double longitude,
        String phone,
        String email,
        String timezone,
        Boolean isPrimary,
        String status,
        Instant createdAt
) {}
