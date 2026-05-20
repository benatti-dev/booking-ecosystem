package com.booker.auth.service;

import com.booker.auth.entity.AuditLog;
import com.booker.auth.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes audit log entries asynchronously in a separate transaction so that
 * audit failures never roll back the calling business transaction.
 *
 * <p>IP resolution is intentionally done in the <em>controller layer</em>
 * (before the servlet container can recycle the request object) and passed
 * here as a plain {@code String}. This avoids reading a recycled
 * {@code HttpServletRequest} from a background thread.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    public static final String ACTION_REGISTER               = "REGISTER";
    public static final String ACTION_LOGIN                  = "LOGIN";
    public static final String ACTION_FAILED_LOGIN           = "FAILED_LOGIN";
    public static final String ACTION_LOGOUT                 = "LOGOUT";
    public static final String ACTION_PASSWORD_RESET_REQUEST = "PASSWORD_RESET_REQUEST";
    public static final String ACTION_PASSWORD_RESET         = "PASSWORD_RESET";

    private final AuditLogRepository auditLogRepository;

    /** Convenience overload — no extra details payload. */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, Long userId, String ipAddress) {
        doLog(action, userId, ipAddress, null);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, Long userId, String ipAddress, String details) {
        doLog(action, userId, ipAddress, details);
    }

    // ── Private helper ─────────────────────────────────────────────────────

    /**
     * Not annotated with @Async / @Transactional so it is always called
     * directly (no self-invocation proxy bypass).  Both public overloads
     * enter through their own proxy-managed annotation processing.
     */
    private void doLog(String action, Long userId, String ipAddress, String details) {
        try {
            AuditLog entry = AuditLog.builder()
                    .action(action)
                    .userId(userId)
                    .ipAddress(ipAddress)
                    .details(details)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            // Audit log failure must never break the main flow
            log.error("Failed to write audit log [action={}]: {}", action, ex.getMessage());
        }
    }
}
