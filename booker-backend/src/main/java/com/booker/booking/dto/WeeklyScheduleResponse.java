package com.booker.booking.dto;

import java.util.List;

public record WeeklyScheduleResponse(
        Long employeeId,
        Long resourceId,
        List<ScheduleRuleDto> rules,
        List<ScheduleOverrideDto> upcomingOverrides
) {}
