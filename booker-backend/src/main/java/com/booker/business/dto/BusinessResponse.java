package com.booker.business.dto;

import com.booker.business.entity.BusinessStatus;

import java.time.Instant;
import java.util.Map;

public record BusinessResponse(
        Long id,
        Long ownerId,
        String ownerName,
        CategorySummary category,
        String name,
        String description,
        String logoUrl,
        BusinessStatus status,
        Map<String, Object> meta,
        Instant createdAt,
        Instant updatedAt
) {
    public record CategorySummary(Long id, String name, String label, String resourceType) {}
}
