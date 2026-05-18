package com.booker.business.dto;

public record CategoryResponse(
        Long id,
        String name,
        String label,
        String iconUrl,
        String resourceType
) {}
