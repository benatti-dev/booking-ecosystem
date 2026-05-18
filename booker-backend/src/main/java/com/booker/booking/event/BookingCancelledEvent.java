package com.booker.booking.event;

import com.booker.booking.entity.Booking;

public record BookingCancelledEvent(Booking booking, String reason) {}
