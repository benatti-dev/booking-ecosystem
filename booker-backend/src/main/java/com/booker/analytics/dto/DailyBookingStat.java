package com.booker.analytics.dto;

import java.math.BigDecimal;

/**
 * Aggregated booking statistics for a single calendar day.
 * Used in the "bookings per day" time-series chart on the business analytics dashboard.
 */
public record DailyBookingStat(
        String day,               // yyyy-MM-dd in the business's local timezone
        long totalBookings,
        BigDecimal revenue        // sum of price_snapshot for COMPLETED bookings on this day
) {}
