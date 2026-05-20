package com.booker.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Mock email service that logs outgoing messages to the application log.
 * No real emails are sent. Replace with SmtpEmailService when a real SMTP
 * relay is available (see features/email-notifications.md).
 */
@Service
@Slf4j
public class MockEmailService implements EmailService {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    @Override
    public void sendBookingCreated(String clientEmail, String serviceName, Instant startTime) {
        log.info("[EMAIL-MOCK] → {} | Booking created — {} on {}",
                clientEmail, serviceName, FMT.format(startTime));
    }

    @Override
    public void sendBookingConfirmed(String clientEmail, String serviceName, Instant startTime) {
        log.info("[EMAIL-MOCK] → {} | Booking CONFIRMED — {} on {}",
                clientEmail, serviceName, FMT.format(startTime));
    }

    @Override
    public void sendBookingCancelled(String clientEmail, String serviceName, String reason) {
        log.info("[EMAIL-MOCK] → {} | Booking CANCELLED — {} — reason: {}",
                clientEmail, serviceName, reason);
    }

    @Override
    public void sendBookingReminder(String clientEmail, String serviceName, Instant startTime) {
        log.info("[EMAIL-MOCK] → {} | REMINDER — {} on {}",
                clientEmail, serviceName, FMT.format(startTime));
    }
}
