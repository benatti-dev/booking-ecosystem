package com.booker.booking.event;

import com.booker.booking.entity.Booking;

public record BookingConfirmedEvent(Booking booking) {}
