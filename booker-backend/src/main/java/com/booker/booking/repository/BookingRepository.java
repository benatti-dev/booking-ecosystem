package com.booker.booking.repository;

import com.booker.booking.entity.Booking;
import com.booker.booking.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByClientIdOrderByStartTimeDesc(Long clientId, Pageable pageable);

    Page<Booking> findByBusinessIdOrderByStartTimeDesc(Long businessId, Pageable pageable);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.employee.id = :employeeId
              AND b.startTime >= :from
              AND b.startTime < :to
              AND b.status NOT IN ('CANCELLED', 'NO_SHOW')
            ORDER BY b.startTime
            """)
    List<Booking> findActiveByEmployeeAndDateRange(
            @Param("employeeId") Long employeeId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.resource.id = :resourceId
              AND b.startTime >= :from
              AND b.startTime < :to
              AND b.status NOT IN ('CANCELLED', 'NO_SHOW')
            ORDER BY b.startTime
            """)
    List<Booking> findActiveByResourceAndDateRange(
            @Param("resourceId") Long resourceId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.business.id = :businessId
              AND b.startTime >= :from
              AND b.startTime < :to
            ORDER BY b.startTime
            """)
    Page<Booking> findByBusinessAndDateRange(
            @Param("businessId") Long businessId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    // For 24h reminder scheduling
    @Query("""
            SELECT b FROM Booking b
            WHERE b.status = 'CONFIRMED'
              AND b.startTime >= :from
              AND b.startTime < :to
            """)
    List<Booking> findConfirmedInRange(@Param("from") Instant from, @Param("to") Instant to);
}
