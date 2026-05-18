package com.booker.booking.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleOverrideDto(
        Long id,
        @NotNull LocalDate overrideDate,
        LocalTime startTime,
        LocalTime endTime,
        String reason
) {}
