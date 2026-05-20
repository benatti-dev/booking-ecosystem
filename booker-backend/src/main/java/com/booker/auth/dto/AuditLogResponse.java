package com.booker.auth.dto;

import com.booker.auth.entity.AuditLog;

import java.time.Instant;

/** Read-only projection of an audit log entry. */
public record AuditLogResponse(
        Long id,
        Long userId,
        String action,
        String ipAddress,
        String details,
        Instant createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getUserId(),
                log.getAction(),
                log.getIpAddress(),
                log.getDetails(),
                log.getCreatedAt()
        );
    }
}
