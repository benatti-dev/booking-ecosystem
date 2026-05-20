package com.booker.booking.event;

import java.time.Instant;

/**
 * Domain event published AFTER a booking is cancelled (transaction committed).
 */
public record BookingCancelledEvent(
        Long bookingId,
        Long clientId,
        String clientEmail,
        Long ownerId,
        String serviceName,
        Instant startTime,
        String reason
) {}
