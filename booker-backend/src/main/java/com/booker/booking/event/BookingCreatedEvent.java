package com.booker.booking.event;

import java.time.Instant;

/**
 * Domain event published AFTER a new booking is created (transaction committed).
 * Carries only scalar data to avoid LazyInitializationException in async listeners.
 */
public record BookingCreatedEvent(
        Long bookingId,
        Long clientId,
        String clientEmail,
        String clientFullName,
        Long businessId,
        Long ownerId,
        String serviceName,
        Instant startTime
) {}
