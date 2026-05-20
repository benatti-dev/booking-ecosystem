package com.booker.analytics.service;

import com.booker.analytics.dto.*;
import com.booker.auth.entity.User;
import com.booker.auth.entity.UserRole;
import com.booker.booking.entity.BookingStatus;
import com.booker.business.entity.Business;
import com.booker.business.entity.BusinessStatus;
import com.booker.business.repository.BusinessRepository;
import com.booker.shared.exception.BookerException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Executes aggregation queries for business-level and platform-level analytics.
 *
 * <p>Native SQL is preferred over JPQL for GROUP BY + aggregate queries because
 * JPA's JPQL has limited support for window functions and date-truncation idioms.</p>
 *
 * <p>The business owner can only view analytics for their own businesses —
 * ownership is verified by the caller ({@link com.booker.analytics.controller.AnalyticsController}).</p>
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final EntityManager em;
    private final BusinessRepository businessRepository;

    // ── Business Analytics ──────────────────────────────────────────────────

    /**
     * Computes the full analytics payload for one business over the last 30 days.
     *
     * <p>Business owners are restricted to their own businesses; admins have unrestricted access.
     * Ownership is enforced here so the controller stays thin.</p>
     *
     * @param businessId target business
     * @param caller     authenticated user requesting the analytics
     * @return analytics response with daily stats, revenue breakdown, utilization, and rates
     */
    @Transactional(readOnly = true)
    public BusinessAnalyticsResponse getBusinessAnalytics(Long businessId, User caller) {
        Business business = businessRepository.findByIdWithDetails(businessId)
                .orElseThrow(() -> BookerException.notFound("Business not found: " + businessId));

        if (caller.getRole() == UserRole.BUSINESS_OWNER
                && !business.getOwner().getId().equals(caller.getId())) {
            throw BookerException.forbidden("Access denied to this business's analytics");
        }

        Instant windowStart = Instant.now().minus(30, ChronoUnit.DAYS);

        List<DailyBookingStat> daily        = queryDailyBookings(businessId, windowStart);
        List<ServiceRevenueStat> revenue    = queryRevenueByService(businessId, windowStart);
        List<EmployeeUtilizationStat> util  = queryEmployeeUtilization(businessId, windowStart);
        BigDecimal cancellationRate         = queryCancellationRate(businessId, windowStart);

        long totalCompleted = revenue.stream().mapToLong(ServiceRevenueStat::bookingCount).sum();
        BigDecimal totalRevenue = revenue.stream()
                .map(ServiceRevenueStat::totalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BusinessAnalyticsResponse(
                businessId,
                business.getName(),
                daily,
                revenue,
                util,
                cancellationRate,
                totalCompleted,
                totalRevenue
        );
    }

    // ── Platform Overview (Admin) ───────────────────────────────────────────

    /**
     * Computes platform-wide KPIs for the admin overview dashboard.
     */
    @Transactional(readOnly = true)
    public PlatformOverviewResponse getPlatformOverview() {
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant sevenDaysAgo  = Instant.now().minus(7,  ChronoUnit.DAYS);

        return new PlatformOverviewResponse(
                countBusinessesByStatus(BusinessStatus.ACTIVE),
                countBusinessesByStatus(BusinessStatus.PENDING),
                countBusinessesByStatus(BusinessStatus.SUSPENDED),
                countBusinessesByStatus(BusinessStatus.REJECTED),
                countUsersByRole(UserRole.CLIENT),
                countUsersByRole(UserRole.BUSINESS_OWNER),
                countUsersByRole(UserRole.EMPLOYEE),
                countUsersByRole(UserRole.ADMIN),
                countBookingsAfter(thirtyDaysAgo),
                countCompletedBookingsAfter(thirtyDaysAgo),
                sumRevenueAfter(thirtyDaysAgo),
                countNewUsersAfter(sevenDaysAgo),
                countNewBusinessesAfter(sevenDaysAgo)
        );
    }

    // ── Private query helpers ───────────────────────────────────────────────

    /**
     * Returns bookings-per-day aggregated in UTC.
     * Only COMPLETED bookings contribute revenue; all statuses are counted.
     */
    @SuppressWarnings("unchecked")
    private List<DailyBookingStat> queryDailyBookings(Long businessId, Instant from) {
        String sql = """
                SELECT TO_CHAR(DATE_TRUNC('day', bk.start_time AT TIME ZONE 'UTC'), 'YYYY-MM-DD') AS day,
                       COUNT(*)                                                                    AS total_bookings,
                       COALESCE(SUM(CASE WHEN bk.status = 'COMPLETED' THEN bk.price_snapshot ELSE 0 END), 0) AS revenue
                FROM bookings bk
                WHERE bk.business_id = :businessId
                  AND bk.start_time  >= :from
                GROUP BY DATE_TRUNC('day', bk.start_time AT TIME ZONE 'UTC')
                ORDER BY day
                """;

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("businessId", businessId)
                .setParameter("from", from)
                .getResultList();

        return rows.stream()
                .map(r -> new DailyBookingStat(
                        (String) r[0],
                        ((Number) r[1]).longValue(),
                        toBigDecimal(r[2])
                ))
                .toList();
    }

    /** Revenue and booking count per service (COMPLETED bookings only). */
    @SuppressWarnings("unchecked")
    private List<ServiceRevenueStat> queryRevenueByService(Long businessId, Instant from) {
        String sql = """
                SELECT s.id           AS service_id,
                       s.name         AS service_name,
                       COUNT(bk.id)   AS booking_count,
                       COALESCE(SUM(bk.price_snapshot), 0) AS total_revenue
                FROM bookings bk
                JOIN services s ON bk.service_id = s.id
                WHERE bk.business_id = :businessId
                  AND bk.status      = 'COMPLETED'
                  AND bk.start_time  >= :from
                GROUP BY s.id, s.name
                ORDER BY total_revenue DESC
                """;

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("businessId", businessId)
                .setParameter("from", from)
                .getResultList();

        return rows.stream()
                .map(r -> new ServiceRevenueStat(
                        ((Number) r[0]).longValue(),
                        (String) r[1],
                        ((Number) r[2]).longValue(),
                        toBigDecimal(r[3])
                ))
                .toList();
    }

    /** Completed bookings per employee (COMPLETED bookings only). */
    @SuppressWarnings("unchecked")
    private List<EmployeeUtilizationStat> queryEmployeeUtilization(Long businessId, Instant from) {
        String sql = """
                SELECT e.id           AS employee_id,
                       e.display_name AS employee_name,
                       COUNT(bk.id)   AS completed_bookings
                FROM bookings bk
                JOIN employees e ON bk.employee_id = e.id
                WHERE bk.business_id = :businessId
                  AND bk.status      = 'COMPLETED'
                  AND bk.start_time  >= :from
                GROUP BY e.id, e.display_name
                ORDER BY completed_bookings DESC
                """;

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("businessId", businessId)
                .setParameter("from", from)
                .getResultList();

        return rows.stream()
                .map(r -> new EmployeeUtilizationStat(
                        ((Number) r[0]).longValue(),
                        (String) r[1],
                        ((Number) r[2]).longValue()
                ))
                .toList();
    }

    /**
     * Calculates the cancellation rate as:
     *   (CANCELLED + NO_SHOW) / total bookings * 100.
     * Returns {@link BigDecimal#ZERO} if there are no bookings in the window.
     */
    private BigDecimal queryCancellationRate(Long businessId, Instant from) {
        String sql = """
                SELECT COUNT(*) FILTER (WHERE status IN ('CANCELLED', 'NO_SHOW')) AS cancelled,
                       COUNT(*)                                                    AS total
                FROM bookings
                WHERE business_id = :businessId
                  AND start_time  >= :from
                """;

        Object[] row = (Object[]) em.createNativeQuery(sql)
                .setParameter("businessId", businessId)
                .setParameter("from", from)
                .getSingleResult();

        long cancelled = ((Number) row[0]).longValue();
        long total     = ((Number) row[1]).longValue();

        if (total == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(cancelled * 100.0 / total)
                .setScale(1, RoundingMode.HALF_UP);
    }

    // ── Platform counters ───────────────────────────────────────────────────

    private long countBusinessesByStatus(BusinessStatus status) {
        return (Long) em.createQuery(
                        "SELECT COUNT(b) FROM Business b WHERE b.status = :status")
                .setParameter("status", status)
                .getSingleResult();
    }

    private long countUsersByRole(UserRole role) {
        return (Long) em.createQuery(
                        "SELECT COUNT(u) FROM User u WHERE u.role = :role")
                .setParameter("role", role)
                .getSingleResult();
    }

    private long countBookingsAfter(Instant from) {
        return (Long) em.createQuery(
                        "SELECT COUNT(b) FROM Booking b WHERE b.startTime >= :from")
                .setParameter("from", from)
                .getSingleResult();
    }

    private long countCompletedBookingsAfter(Instant from) {
        return (Long) em.createQuery(
                        "SELECT COUNT(b) FROM Booking b WHERE b.startTime >= :from AND b.status = :status")
                .setParameter("from", from)
                .setParameter("status", BookingStatus.COMPLETED)
                .getSingleResult();
    }

    private BigDecimal sumRevenueAfter(Instant from) {
        BigDecimal result = (BigDecimal) em.createQuery(
                        "SELECT COALESCE(SUM(b.priceSnapshot), 0) FROM Booking b " +
                        "WHERE b.startTime >= :from AND b.status = :status")
                .setParameter("from", from)
                .setParameter("status", BookingStatus.COMPLETED)
                .getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }

    private long countNewUsersAfter(Instant from) {
        return (Long) em.createQuery(
                        "SELECT COUNT(u) FROM User u WHERE u.createdAt >= :from")
                .setParameter("from", from)
                .getSingleResult();
    }

    private long countNewBusinessesAfter(Instant from) {
        return (Long) em.createQuery(
                        "SELECT COUNT(b) FROM Business b WHERE b.createdAt >= :from")
                .setParameter("from", from)
                .getSingleResult();
    }

    // ── Utility ─────────────────────────────────────────────────────────────

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }
}
