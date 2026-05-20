package com.booker.analytics.dto;

import java.math.BigDecimal;

/**
 * Revenue and booking count grouped by service name.
 * Drives the "revenue by service" pie/bar chart on the business analytics dashboard.
 */
public record ServiceRevenueStat(
        Long serviceId,
        String serviceName,
        long bookingCount,
        BigDecimal totalRevenue
) {}
