package com.booker.business.dto;

import jakarta.validation.constraints.Size;

import java.util.Map;

public record UpdateBusinessRequest(
        @Size(max = 255) String name,
        String description,
        String logoUrl,
        Map<String, Object> meta
) {}
