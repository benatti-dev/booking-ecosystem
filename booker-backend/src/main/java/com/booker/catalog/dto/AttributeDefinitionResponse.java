package com.booker.catalog.dto;

import com.booker.catalog.entity.AttributeFieldType;

import java.util.List;

public record AttributeDefinitionResponse(
        Long id,
        Long categoryId,
        String fieldKey,
        String fieldLabel,
        AttributeFieldType fieldType,
        List<String> options,
        Boolean isRequired,
        Integer sortOrder
) {}
