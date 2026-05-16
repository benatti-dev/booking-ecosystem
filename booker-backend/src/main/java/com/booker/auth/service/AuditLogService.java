package com.booker.auth.service;

import com.booker.auth.entity.AuditLog;
import com.booker.auth.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    public static final String ACTION_REGISTER        = "REGISTER";
    public static final String ACTION_LOGIN           = "LOGIN";
    public static final String ACTION_FAILED_LOGIN    = "FAILED_LOGIN";
    public static final String ACTION_LOGOUT          = "LOGOUT";
    public static final String ACTION_PASSWORD_RESET_REQUEST = "PASSWORD_RESET_REQUEST";
    public static final String ACTION_PASSWORD_RESET  = "PASSWORD_RESET";

    private final AuditLogRepository auditLogRepository;

    @Async
    public void log(String action, Long userId, HttpServletRequest request) {
        log(action, userId, request, null);
    }

    @Async
    public void log(String action, Long userId, HttpServletRequest request, String details) {
        try {
            AuditLog entry = AuditLog.builder()
                    .action(action)
                    .userId(userId)
                    .ipAddress(resolveIp(request))
                    .details(details)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            // Audit log failure must never break the main flow
            log.error("Failed to write audit log [action={}]: {}", action, ex.getMessage());
        }
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) return null;
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
