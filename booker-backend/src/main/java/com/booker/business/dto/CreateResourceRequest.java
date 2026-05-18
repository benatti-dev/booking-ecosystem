package com.booker.business.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record CreateResourceRequest(
        @NotNull Long branchId,
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 100) String resourceType,
        @Min(1) int capacity,
        Map<String, Object> meta
) {}
