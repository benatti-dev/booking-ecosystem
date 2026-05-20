package com.booker.notification.service;

import java.time.Instant;

/**
 * Abstraction over email delivery. The default active bean is {@link MockEmailService}
 * which logs messages only. To switch to real SMTP delivery, configure the environment
 * variables MAIL_HOST / MAIL_PORT / MAIL_USER / MAIL_PASS and deploy {@code SmtpEmailService}.
 * See {@code features/email-notifications.md} for the full migration guide.
 */
public interface EmailService {

    void sendBookingCreated(String clientEmail, String serviceName, Instant startTime);

    void sendBookingConfirmed(String clientEmail, String serviceName, Instant startTime);

    void sendBookingCancelled(String clientEmail, String serviceName, String reason);

    void sendBookingReminder(String clientEmail, String serviceName, Instant startTime);
}
