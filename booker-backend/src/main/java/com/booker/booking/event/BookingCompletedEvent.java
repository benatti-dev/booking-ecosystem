package com.booker.booking.event;

import java.time.Instant;

/**
 * Domain event published AFTER a booking is completed (transaction committed).
 */
public record BookingCompletedEvent(
        Long bookingId,
        Long clientId,
        String clientEmail,
        Long ownerId,
        String serviceName,
        Instant startTime
) {}
