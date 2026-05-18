package com.booker.catalog.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record ServiceResponse(
        Long id,
        Long businessId,
        Long categoryId,
        String categoryName,
        String name,
        String description,
        Integer durationMin,
        BigDecimal price,
        String currency,
        Map<String, Object> attributes,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {}
