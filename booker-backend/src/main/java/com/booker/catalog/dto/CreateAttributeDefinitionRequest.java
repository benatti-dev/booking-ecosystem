package com.booker.catalog.dto;

import com.booker.catalog.entity.AttributeFieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateAttributeDefinitionRequest(
        @NotBlank @Size(max = 100) String fieldKey,
        @NotBlank @Size(max = 100) String fieldLabel,
        @NotNull AttributeFieldType fieldType,
        List<String> options,
        Boolean isRequired,
        Integer sortOrder
) {}
