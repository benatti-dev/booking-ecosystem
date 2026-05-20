package com.booker.notification.service;

import com.booker.booking.event.BookingCancelledEvent;
import com.booker.booking.event.BookingCompletedEvent;
import com.booker.booking.event.BookingConfirmedEvent;
import com.booker.booking.event.BookingCreatedEvent;
import com.booker.notification.dto.NotificationResponse;
import com.booker.notification.entity.Notification;
import com.booker.notification.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Listens to booking domain events and creates notifications + sends mock emails.
 * Uses {@code @TransactionalEventListener} so events fire only AFTER the source
 * transaction commits — prevents data visibility issues and detached entities.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventListener {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM, HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final NotificationService notificationService;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener
    @Async
    public void onBookingCreated(BookingCreatedEvent event) {
        log.debug("[WS] onBookingCreated fired — bookingId={} clientId={} ownerId={}",
                event.bookingId(), event.clientId(), event.ownerId());
        String time = FMT.format(event.startTime());

        // Notify client
        Notification clientNotif = notificationService.create(
                event.clientId(),
                NotificationType.BOOKING_CREATED,
                "Booking Created",
                "Your booking for \"" + event.serviceName() + "\" on " + time + " is awaiting confirmation.",
                event.bookingId()
        );
        pushToUser(event.clientId(), notificationService.toResponse(clientNotif));

        // Notify business owner
        if (event.ownerId() != null) {
            Notification ownerNotif = notificationService.create(
                    event.ownerId(),
                    NotificationType.BOOKING_CREATED,
                    "New Booking Received",
                    "Client " + event.clientFullName() + " booked \""
                            + event.serviceName() + "\" on " + time + ".",
                    event.bookingId()
            );
            pushToUser(event.ownerId(), notificationService.toResponse(ownerNotif));
        }

        emailService.sendBookingCreated(event.clientEmail(), event.serviceName(), event.startTime());
    }

    @TransactionalEventListener
    @Async
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        log.debug("[WS] onBookingConfirmed fired — bookingId={} clientId={} ownerId={}",
                event.bookingId(), event.clientId(), event.ownerId());
        String time = FMT.format(event.startTime());

        // Notify client
        Notification n = notificationService.create(
                event.clientId(),
                NotificationType.BOOKING_CONFIRMED,
                "Booking Confirmed",
                "Your booking for \"" + event.serviceName() + "\" on " + time + " has been confirmed.",
                event.bookingId()
        );
        pushToUser(event.clientId(), notificationService.toResponse(n));

        // Notify business owner that they confirmed (useful for multi-staff setups)
        if (event.ownerId() != null && !event.ownerId().equals(event.clientId())) {
            Notification ownerNotif = notificationService.create(
                    event.ownerId(),
                    NotificationType.BOOKING_CONFIRMED,
                    "Booking Confirmed",
                    "You confirmed booking for \"" + event.serviceName() + "\" on " + time + ".",
                    event.bookingId()
            );
            pushToUser(event.ownerId(), notificationService.toResponse(ownerNotif));
        }

        emailService.sendBookingConfirmed(event.clientEmail(), event.serviceName(), event.startTime());
    }

    @TransactionalEventListener
    @Async
    public void onBookingCancelled(BookingCancelledEvent event) {
        log.debug("[WS] onBookingCancelled fired — bookingId={} clientId={} ownerId={}",
                event.bookingId(), event.clientId(), event.ownerId());

        // Notify client
        Notification n = notificationService.create(
                event.clientId(),
                NotificationType.BOOKING_CANCELLED,
                "Booking Cancelled",
                "Your booking for \"" + event.serviceName()
                        + "\" has been cancelled. Reason: " + event.reason(),
                event.bookingId()
        );
        pushToUser(event.clientId(), notificationService.toResponse(n));

        // Notify business owner that client cancelled
        if (event.ownerId() != null && !event.ownerId().equals(event.clientId())) {
            Notification ownerNotif = notificationService.create(
                    event.ownerId(),
                    NotificationType.BOOKING_CANCELLED,
                    "Booking Cancelled by Client",
                    "A booking for \"" + event.serviceName() + "\" has been cancelled."
                            + (event.reason() != null ? " Reason: " + event.reason() : ""),
                    event.bookingId()
            );
            pushToUser(event.ownerId(), notificationService.toResponse(ownerNotif));
        }

        emailService.sendBookingCancelled(event.clientEmail(), event.serviceName(), event.reason());
    }

    @TransactionalEventListener
    @Async
    public void onBookingCompleted(BookingCompletedEvent event) {
        log.debug("[WS] onBookingCompleted fired — bookingId={} clientId={} ownerId={}",
                event.bookingId(), event.clientId(), event.ownerId());
        String time = FMT.format(event.startTime());

        // Notify client
        Notification clientNotif = notificationService.create(
                event.clientId(),
                NotificationType.BOOKING_CONFIRMED,
                "Service Completed",
                "Your booking for \"" + event.serviceName() + "\" on " + time + " has been marked as completed.",
                event.bookingId()
        );
        pushToUser(event.clientId(), notificationService.toResponse(clientNotif));
    }

    private void pushToUser(Long userId, NotificationResponse dto) {
        log.debug("[WS] Pushing notification to userId={} type={}", userId, dto.type());
        try {
            messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", dto);
            log.debug("[WS] Push succeeded for userId={}", userId);
        } catch (Exception ex) {
            log.warn("WebSocket push failed for user {}: {}", userId, ex.getMessage());
        }
    }
}
