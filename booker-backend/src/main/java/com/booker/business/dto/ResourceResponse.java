package com.booker.business.dto;

import java.util.Map;

public record ResourceResponse(
        Long id,
        Long businessId,
        Long branchId,
        String name,
        String resourceType,
        int capacity,
        Map<String, Object> meta,
        boolean isActive
) {}
