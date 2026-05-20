package com.booker.auth.controller;

import com.booker.auth.dto.AuditLogResponse;
import com.booker.auth.dto.UserResponse;
import com.booker.auth.dto.UserStatusRequest;
import com.booker.auth.entity.UserRole;
import com.booker.auth.entity.UserStatus;
import com.booker.auth.entity.User;
import com.booker.auth.repository.UserRepository;
import com.booker.auth.service.AdminService;
import com.booker.shared.dto.PagedResponse;
import com.booker.shared.exception.BookerException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * Admin-only endpoints for user management and audit log access.
 *
 * <p>Business management (approve/reject/suspend) is already handled by
 * {@link com.booker.business.controller.BusinessController}. This controller
 * focuses on user lifecycle and platform audit trail.</p>
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin — Users & Audit", description = "User management and audit log (ADMIN only)")
public class AdminController {

    private final UserRepository userRepository;
    private final AdminService adminService;

    // ── Users ──────────────────────────────────────────────────────────────

    /**
     * Lists all platform users with optional filters by role and status.
     * Null parameters are excluded from the query to avoid PostgreSQL type inference errors.
     *
     * @param role   optional filter (CLIENT, BUSINESS_OWNER, EMPLOYEE, ADMIN)
     * @param status optional filter (ACTIVE, SUSPENDED)
     */
    @GetMapping("/users")
    @Operation(summary = "List all users (paginated, filterable)")
    public ResponseEntity<PagedResponse<UserResponse>> listUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        return ResponseEntity.ok(
                PagedResponse.of(adminService.listUsers(role, status, pageable).map(UserResponse::from))
        );
    }

    /**
     * Changes the status of a user account (e.g., suspend or restore).
     *
     * <p>An admin cannot modify another admin account to prevent privilege escalation.</p>
     *
     * @param id  target user ID
     * @param req new status
     */
    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Suspend or restore a user account")
    public ResponseEntity<UserResponse> changeUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest req) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> BookerException.notFound("User not found: " + id));

        if (user.getRole() == UserRole.ADMIN) {
            throw BookerException.forbidden("Admin accounts cannot be modified via this endpoint");
        }

        user.setStatus(req.status());
        return ResponseEntity.ok(UserResponse.from(userRepository.save(user)));
    }

    // ── Audit Logs ─────────────────────────────────────────────────────────

    /**
     * Paginates the platform audit log with optional filters.
     * Null parameters are excluded from the query to avoid PostgreSQL type inference errors.
     *
     * @param action optional exact action name (e.g., LOGIN, REGISTER)
     * @param from   optional lower-bound timestamp (ISO 8601)
     * @param to     optional upper-bound timestamp (ISO 8601)
     */
    @GetMapping("/audit-logs")
    @Operation(summary = "Query audit logs with optional filters")
    public ResponseEntity<PagedResponse<AuditLogResponse>> listAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {

        return ResponseEntity.ok(
                PagedResponse.of(adminService.listAuditLogs(action, from, to, pageable).map(AuditLogResponse::from))
        );
    }
}

