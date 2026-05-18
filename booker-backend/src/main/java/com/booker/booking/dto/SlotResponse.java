package com.booker.booking.dto;

import java.time.LocalTime;
import java.util.List;

public record SlotResponse(
        String date,
        Long employeeId,
        Long resourceId,
        Long serviceId,
        int durationMin,
        List<LocalTime> availableSlots
) {}
