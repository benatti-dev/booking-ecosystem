package com.booker.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

public record CreateBookingRequest(
        @NotNull Long serviceId,
        Long employeeId,
        Long resourceId,
        @NotNull Long branchId,
        @NotNull @Future Instant startTime,
        String clientNote,
        Map<String, Object> selectedAttributes
) {}
