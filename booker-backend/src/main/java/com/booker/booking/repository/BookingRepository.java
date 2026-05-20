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
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Eagerly loads all associations needed by BookingResponse to avoid N+1 queries.
     */
    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.client
            JOIN FETCH b.service
            JOIN FETCH b.business
            JOIN FETCH b.branch
            LEFT JOIN FETCH b.employee
            LEFT JOIN FETCH b.resource
            WHERE b.id = :id
            """)
    Optional<Booking> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.client
            JOIN FETCH b.service
            JOIN FETCH b.business
            JOIN FETCH b.branch
            LEFT JOIN FETCH b.employee
            LEFT JOIN FETCH b.resource
            WHERE b.client.id = :clientId
            ORDER BY b.startTime DESC
            """)
    Page<Booking> findByClientIdOrderByStartTimeDesc(@Param("clientId") Long clientId, Pageable pageable);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.client
            JOIN FETCH b.service
            JOIN FETCH b.business
            JOIN FETCH b.branch
            LEFT JOIN FETCH b.employee
            LEFT JOIN FETCH b.resource
            WHERE b.business.id = :businessId
            ORDER BY b.startTime DESC
            """)
    Page<Booking> findByBusinessIdOrderByStartTimeDesc(@Param("businessId") Long businessId, Pageable pageable);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.client
            JOIN FETCH b.service
            JOIN FETCH b.business
            JOIN FETCH b.branch
            LEFT JOIN FETCH b.employee
            LEFT JOIN FETCH b.resource
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
            JOIN FETCH b.client
            JOIN FETCH b.service
            JOIN FETCH b.business
            JOIN FETCH b.branch
            LEFT JOIN FETCH b.employee
            LEFT JOIN FETCH b.resource
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
            JOIN FETCH b.client
            JOIN FETCH b.service
            JOIN FETCH b.business
            JOIN FETCH b.branch
            LEFT JOIN FETCH b.employee
            LEFT JOIN FETCH b.resource
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
            JOIN FETCH b.client
            JOIN FETCH b.service
            WHERE b.status = 'CONFIRMED'
              AND b.startTime >= :from
              AND b.startTime < :to
            """)
    List<Booking> findConfirmedInRange(@Param("from") Instant from, @Param("to") Instant to);
}
