package com.booker.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record CreateBusinessRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 255) String name,
        String description,
        String logoUrl,
        Map<String, Object> meta
) {}
