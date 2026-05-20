package com.booker.auth.service;

import com.booker.auth.entity.AuditLog;
import com.booker.auth.entity.User;
import com.booker.auth.entity.UserRole;
import com.booker.auth.entity.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles admin-level queries that require dynamic predicate building.
 *
 * <p>Using dynamic JPQL instead of {@code :param IS NULL OR col = :param} patterns
 * avoids the PostgreSQL "could not determine data type of parameter" error that
 * occurs when typed parameters (e.g., Instant, Enum) are bound as NULL — the JDBC
 * driver cannot infer the SQL type from a null value.</p>
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final EntityManager em;

    // ── Users ──────────────────────────────────────────────────────────────

    /**
     * Returns a page of users filtered only by the parameters that are non-null.
     * Null parameters are simply omitted from the WHERE clause.
     */
    @Transactional(readOnly = true)
    public Page<User> listUsers(UserRole role, UserStatus status, Pageable pageable) {
        List<String> predicates = new ArrayList<>();
        if (role   != null) predicates.add("u.role = :role");
        if (status != null) predicates.add("u.status = :status");

        String where = predicates.isEmpty() ? "" : " WHERE " + String.join(" AND ", predicates);

        TypedQuery<User> dataQuery = em.createQuery(
                "SELECT u FROM User u" + where + " ORDER BY u.createdAt DESC", User.class);
        TypedQuery<Long> countQuery = em.createQuery(
                "SELECT COUNT(u) FROM User u" + where, Long.class);

        if (role   != null) { dataQuery.setParameter("role",   role);   countQuery.setParameter("role",   role); }
        if (status != null) { dataQuery.setParameter("status", status); countQuery.setParameter("status", status); }

        long total = countQuery.getSingleResult();

        dataQuery.setFirstResult((int) pageable.getOffset());
        dataQuery.setMaxResults(pageable.getPageSize());

        return new PageImpl<>(dataQuery.getResultList(), pageable, total);
    }

    // ── Audit Logs ─────────────────────────────────────────────────────────

    /**
     * Returns a page of audit log entries filtered only by the parameters that are non-null.
     * Null parameters are simply omitted from the WHERE clause.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> listAuditLogs(String action, Instant from, Instant to, Pageable pageable) {
        List<String> predicates = new ArrayList<>();
        if (action != null) predicates.add("a.action = :action");
        if (from   != null) predicates.add("a.createdAt >= :from");
        if (to     != null) predicates.add("a.createdAt <= :to");

        String where = predicates.isEmpty() ? "" : " WHERE " + String.join(" AND ", predicates);

        TypedQuery<AuditLog> dataQuery = em.createQuery(
                "SELECT a FROM AuditLog a" + where + " ORDER BY a.createdAt DESC", AuditLog.class);
        TypedQuery<Long> countQuery = em.createQuery(
                "SELECT COUNT(a) FROM AuditLog a" + where, Long.class);

        if (action != null) { dataQuery.setParameter("action", action); countQuery.setParameter("action", action); }
        if (from   != null) { dataQuery.setParameter("from",   from);   countQuery.setParameter("from",   from); }
        if (to     != null) { dataQuery.setParameter("to",     to);     countQuery.setParameter("to",     to); }

        long total = countQuery.getSingleResult();

        dataQuery.setFirstResult((int) pageable.getOffset());
        dataQuery.setMaxResults(pageable.getPageSize());

        return new PageImpl<>(dataQuery.getResultList(), pageable, total);
    }
}
