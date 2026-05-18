package com.booker.booking.dto;

import com.booker.booking.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record BookingResponse(
        Long id,
        Long clientId,
        String clientName,
        Long serviceId,
        String serviceName,
        Long businessId,
        String businessName,
        Long branchId,
        String branchName,
        Long employeeId,
        String employeeName,
        Long resourceId,
        String resourceName,
        Instant startTime,
        Instant endTime,
        BookingStatus status,
        String clientNote,
        String businessNote,
        BigDecimal priceSnapshot,
        int durationMin,
        Map<String, Object> selectedAttributes,
        Instant createdAt
) {}
