package com.booker.booking.dto;

import java.time.LocalTime;

public record ScheduleBreakDto(
        Long id,
        LocalTime startTime,
        LocalTime endTime
) {}
