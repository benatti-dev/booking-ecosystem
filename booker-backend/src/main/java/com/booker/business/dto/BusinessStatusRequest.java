package com.booker.business.dto;

import com.booker.business.entity.BusinessStatus;
import jakarta.validation.constraints.NotNull;

public record BusinessStatusRequest(
        @NotNull BusinessStatus status,
        String reason
) {}
