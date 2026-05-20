package com.booker.notification.dto;

import com.booker.notification.entity.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String body,
        Long referenceId,
        boolean isRead,
        Instant createdAt
) {}
