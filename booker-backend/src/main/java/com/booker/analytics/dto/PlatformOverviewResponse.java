package com.booker.analytics.dto;

import java.math.BigDecimal;

/**
 * Platform-wide statistics for the admin overview dashboard.
 * Aggregates across all tenants and all time, with a 30-day revenue window.
 */
public record PlatformOverviewResponse(
        // Business counts by status
        long totalBusinessesActive,
        long totalBusinessesPending,
        long totalBusinessesSuspended,
        long totalBusinessesRejected,

        // User counts by role
        long totalClients,
        long totalBusinessOwners,
        long totalEmployees,
        long totalAdmins,

        // Booking KPIs (last 30 days)
        long bookingsLast30Days,
        long completedBookingsLast30Days,
        BigDecimal revenueLast30Days,

        // Growth
        long newUsersLast7Days,
        long newBusinessesLast7Days
) {}
