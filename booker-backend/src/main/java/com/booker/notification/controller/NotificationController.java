package com.booker.notification.controller;

import com.booker.auth.entity.User;
import com.booker.notification.dto.NotificationResponse;
import com.booker.notification.service.NotificationService;
import com.booker.shared.dto.PagedResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public PagedResponse<NotificationResponse> getMyNotifications(
            Authentication auth,
            @PageableDefault(size = 20) Pageable pageable) {
        User user = (User) auth.getPrincipal();
        return PagedResponse.of(notificationService.getForUser(user.getId(), pageable));
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return Map.of("count", notificationService.countUnread(user.getId()));
    }

    @PatchMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id, Authentication auth) {
        User user = (User) auth.getPrincipal();
        notificationService.markAsRead(id, user.getId());
    }

    @PatchMapping("/read-all")
    public void markAllAsRead(Authentication auth) {
        User user = (User) auth.getPrincipal();
        notificationService.markAllAsRead(user.getId());
    }
}
