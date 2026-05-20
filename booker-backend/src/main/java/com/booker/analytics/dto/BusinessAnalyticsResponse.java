package com.booker.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Composite analytics payload for a single business (last 30 days).
 * All time-series data is expressed in the business's configured timezone.
 */
public record BusinessAnalyticsResponse(
        Long businessId,
        String businessName,

        /** Bookings per day for the last 30 days (including days with zero bookings). */
        List<DailyBookingStat> dailyBookings,

        /** Revenue breakdown per service (COMPLETED bookings only). */
        List<ServiceRevenueStat> revenueByService,

        /** Completed bookings per employee (COMPLETED bookings only). */
        List<EmployeeUtilizationStat> employeeUtilization,

        /** Percentage of bookings that were cancelled or no-showed. */
        BigDecimal cancellationRate,

        /** Total completed bookings in the period. */
        long totalCompletedBookings,

        /** Total revenue from COMPLETED bookings in the period. */
        BigDecimal totalRevenue
) {}
