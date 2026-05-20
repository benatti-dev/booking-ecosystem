package com.booker.booking.event;

import java.time.Instant;

/**
 * Domain event published AFTER a booking is confirmed (transaction committed).
 */
public record BookingConfirmedEvent(
        Long bookingId,
        Long clientId,
        String clientEmail,
        Long ownerId,
        String serviceName,
        Instant startTime
) {}
