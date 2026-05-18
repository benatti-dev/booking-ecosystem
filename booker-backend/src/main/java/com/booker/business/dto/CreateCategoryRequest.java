package com.booker.business.dto;

import com.booker.business.entity.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String label,
        String iconUrl,
        @NotNull ResourceType resourceType
) {}
