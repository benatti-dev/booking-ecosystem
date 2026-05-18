package com.booker.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

public record CreateServiceRequest(
        @NotBlank @Size(max = 255) String name,
        String description,
        @NotNull @Positive Integer durationMin,
        BigDecimal price,
        @Size(max = 3) String currency,
        Map<String, Object> attributes
) {}
