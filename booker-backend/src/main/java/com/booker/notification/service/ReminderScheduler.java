package com.booker.notification.service;

import com.booker.booking.entity.Booking;
import com.booker.booking.repository.BookingRepository;
import com.booker.notification.entity.NotificationType;
import com.booker.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Sends 24-hour booking reminder notifications.
 * Runs every 15 minutes, searching for CONFIRMED bookings starting in [now+24h, now+24h+15m).
 * Idempotent: skips bookings that already have a BOOKING_REMINDER notification.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM, HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedRate = 15 * 60_000)   // every 15 minutes
    @Transactional
    public void sendReminders() {
        Instant from = Instant.now().plus(24, ChronoUnit.HOURS);
        Instant to   = from.plus(15, ChronoUnit.MINUTES);

        List<Booking> upcoming = bookingRepository.findConfirmedInRange(from, to);
        log.debug("Reminder check: {} confirmed bookings in [{}, {})", upcoming.size(), from, to);

        for (Booking b : upcoming) {
            try {
                // Idempotency: skip if already reminded for this booking
                if (notificationRepository.existsByReferenceIdAndType(b.getId(), NotificationType.BOOKING_REMINDER)) {
                    log.debug("Reminder already sent for booking #{}, skipping", b.getId());
                    continue;
                }

                var notif = notificationService.create(
                        b.getClient().getId(),
                        NotificationType.BOOKING_REMINDER,
                        "Reminder: Appointment Tomorrow",
                        "You have \"" + b.getService().getName() + "\" at "
                                + FMT.format(b.getStartTime()) + ". See you soon!",
                        b.getId()
                );
                messagingTemplate.convertAndSendToUser(
                        b.getClient().getId().toString(),
                        "/queue/notifications",
                        notificationService.toResponse(notif)
                );
                emailService.sendBookingReminder(b.getClient().getEmail(), b.getService().getName(), b.getStartTime());
                log.info("Sent 24h reminder for booking #{}", b.getId());
            } catch (Exception ex) {
                log.error("Failed to send reminder for booking #{}: {}", b.getId(), ex.getMessage());
            }
        }
    }
}
