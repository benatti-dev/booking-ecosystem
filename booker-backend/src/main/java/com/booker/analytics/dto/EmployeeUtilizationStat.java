package com.booker.analytics.dto;

/**
 * Completed booking count per employee over the last 30 days.
 * Drives the "employee utilization" bar chart.
 */
public record EmployeeUtilizationStat(
        Long employeeId,
        String employeeName,
        long completedBookings
) {}
