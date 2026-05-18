package com.booker.booking.dto;

import java.time.LocalTime;
import java.util.List;

public record ScheduleRuleDto(
        Long id,
        int dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        boolean isWorkingDay,
        List<ScheduleBreakDto> breaks
) {}
