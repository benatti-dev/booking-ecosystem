package com.booker.notification.service;

import com.booker.auth.repository.UserRepository;
import com.booker.notification.dto.NotificationResponse;
import com.booker.notification.entity.Notification;
import com.booker.notification.entity.NotificationType;
import com.booker.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public Notification create(Long userId, NotificationType type, String title, String body, Long referenceId) {
        return notificationRepository.save(
                Notification.builder()
                        .user(userRepository.getReferenceById(userId))
                        .type(type)
                        .title(title)
                        .body(body)
                        .referenceId(referenceId)
                        .build()
        );
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getForUser(Long userId, Pageable pageable) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .ifPresent(n -> {
                    n.setRead(true);
                    notificationRepository.save(n);
                });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
    }

    public NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getTitle(), n.getBody(),
                n.getReferenceId(), n.isRead(), n.getCreatedAt()
        );
    }
}
