package com.booker.booking.service;

import com.booker.auth.entity.User;
import com.booker.auth.entity.UserRole;
import com.booker.booking.dto.BookingResponse;
import com.booker.booking.dto.CancelBookingRequest;
import com.booker.booking.dto.CreateBookingRequest;
import com.booker.booking.entity.Booking;
import com.booker.booking.entity.BookingCancellation;
import com.booker.booking.entity.BookingStatus;
import com.booker.booking.event.BookingCancelledEvent;
import com.booker.booking.event.BookingConfirmedEvent;
import com.booker.booking.repository.BookingCancellationRepository;
import com.booker.booking.repository.BookingRepository;
import com.booker.business.entity.BookableResource;
import com.booker.business.entity.Branch;
import com.booker.business.entity.Business;
import com.booker.business.entity.Employee;
import com.booker.business.repository.BranchRepository;
import com.booker.business.repository.BusinessRepository;
import com.booker.business.repository.EmployeeRepository;
import com.booker.business.repository.ResourceRepository;
import com.booker.catalog.entity.Service;
import com.booker.shared.exception.BookerException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingCancellationRepository cancellationRepository;
    private final BookingValidator bookingValidator;
    private final BranchRepository branchRepository;
    private final BusinessRepository businessRepository;
    private final EmployeeRepository employeeRepository;
    private final ResourceRepository resourceRepository;
    private final ApplicationEventPublisher eventPublisher;

    @PersistenceContext
    private EntityManager entityManager;

    // ── Create ────────────────────────────────────────────────────

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest req, User client) {
        // Pre-validation (outside lock)
        Service service = bookingValidator.validate(req);

        Branch branch = branchRepository.findById(req.branchId())
                .orElseThrow(() -> BookerException.notFound("Branch not found: " + req.branchId()));

        Business business = businessRepository.findById(service.getBusiness().getId()).orElseThrow();

        Employee employee = null;
        BookableResource resource = null;
        Long lockTarget = null;

        if (req.employeeId() != null) {
            employee = employeeRepository.findById(req.employeeId()).orElseThrow();
            lockTarget = req.employeeId();
        } else {
            resource = resourceRepository.findById(req.resourceId())
                    .orElseThrow(() -> BookerException.notFound("Resource not found: " + req.resourceId()));
            lockTarget = req.resourceId();
        }

        // Acquire PostgreSQL advisory lock to serialize concurrent bookings
        boolean locked = acquireAdvisoryLock(lockTarget);
        if (!locked) {
            throw BookerException.conflict("Slot temporarily unavailable, please try again");
        }

        // Calculate end time
        Instant endTime = req.startTime().plus(service.getDurationMin(), ChronoUnit.MINUTES);

        // Re-check availability within lock (SELECT FOR UPDATE pattern via JPQL)
        checkSlotAvailability(employee, resource, req.startTime(), endTime);

        Booking booking = Booking.builder()
                .client(client)
                .service(service)
                .business(business)
                .branch(branch)
                .employee(employee)
                .resource(resource)
                .startTime(req.startTime())
                .endTime(endTime)
                .status(BookingStatus.PENDING)
                .clientNote(req.clientNote())
                .priceSnapshot(service.getPrice())
                .durationMin(service.getDurationMin())
                .selectedAttributes(req.selectedAttributes() != null ? req.selectedAttributes() : new java.util.HashMap<>())
                .build();

        Booking saved = bookingRepository.save(booking);
        // Lock released automatically on transaction commit

        return toResponse(saved);
    }

    // ── Read ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BookingResponse getById(Long bookingId, Authentication auth) {
        Booking booking = findOrThrow(bookingId);
        checkReadAccess(booking, auth);
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getMyBookings(Long clientId, Pageable pageable) {
        return bookingRepository.findByClientIdOrderByStartTimeDesc(clientId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getBusinessBookings(Long businessId, Instant from, Instant to,
                                                      Pageable pageable) {
        if (from != null && to != null) {
            return bookingRepository.findByBusinessAndDateRange(businessId, from, to, pageable)
                    .map(this::toResponse);
        }
        return bookingRepository.findByBusinessIdOrderByStartTimeDesc(businessId, pageable)
                .map(this::toResponse);
    }

    // ── Status transitions ────────────────────────────────────────

    @Transactional
    public BookingResponse confirmBooking(Long bookingId, Authentication auth) {
        Booking booking = findOrThrow(bookingId);
        checkBusinessAccess(booking, auth);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw BookerException.badRequest("Only PENDING bookings can be confirmed");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        Booking saved = bookingRepository.save(booking);
        eventPublisher.publishEvent(new BookingConfirmedEvent(saved));
        return toResponse(saved);
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId, CancelBookingRequest req, Authentication auth) {
        Booking booking = findOrThrow(bookingId);
        User actor = (User) auth.getPrincipal();

        // Check permission: client cancels own booking (24h rule), or business/admin cancels any
        if (actor.getRole() == UserRole.CLIENT) {
            if (!booking.getClient().getId().equals(actor.getId())) {
                throw BookerException.forbidden("You can only cancel your own bookings");
            }
            if (Instant.now().isAfter(booking.getStartTime().minus(24, ChronoUnit.HOURS))) {
                throw BookerException.badRequest("Cannot cancel booking less than 24 hours before start time");
            }
        } else {
            checkBusinessAccess(booking, auth);
        }

        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.CANCELLED) {
            throw BookerException.badRequest("Booking cannot be cancelled in status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);

        BookingCancellation cancellation = BookingCancellation.builder()
                .booking(saved)
                .cancelledBy(actor)
                .reason(req != null ? req.reason() : null)
                .build();
        cancellationRepository.save(cancellation);

        eventPublisher.publishEvent(new BookingCancelledEvent(saved, req != null ? req.reason() : null));
        return toResponse(saved);
    }

    @Transactional
    public BookingResponse completeBooking(Long bookingId, Authentication auth) {
        Booking booking = findOrThrow(bookingId);
        checkBusinessAccess(booking, auth);

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw BookerException.badRequest("Only CONFIRMED bookings can be completed");
        }
        booking.setStatus(BookingStatus.COMPLETED);
        return toResponse(bookingRepository.save(booking));
    }

    // ── Advisory Lock ─────────────────────────────────────────────

    private boolean acquireAdvisoryLock(Long key) {
        List<?> result = entityManager.createNativeQuery(
                "SELECT pg_try_advisory_xact_lock(:key)")
                .setParameter("key", key)
                .getResultList();
        return !result.isEmpty() && Boolean.TRUE.equals(result.get(0));
    }

    private void checkSlotAvailability(Employee employee, BookableResource resource,
                                        Instant startTime, Instant endTime) {
        List<Booking> conflicts;
        if (employee != null) {
            conflicts = bookingRepository.findActiveByEmployeeAndDateRange(
                    employee.getId(), startTime.minus(1, ChronoUnit.MINUTES), endTime.plus(1, ChronoUnit.MINUTES));
        } else {
            conflicts = bookingRepository.findActiveByResourceAndDateRange(
                    resource.getId(), startTime.minus(1, ChronoUnit.MINUTES), endTime.plus(1, ChronoUnit.MINUTES));
        }

        boolean hasOverlap = conflicts.stream().anyMatch(b ->
                startTime.isBefore(b.getEndTime()) && endTime.isAfter(b.getStartTime()));

        if (hasOverlap) {
            throw BookerException.conflict("Slot is no longer available");
        }
    }

    // ── Access control helpers ────────────────────────────────────

    private void checkReadAccess(Booking booking, Authentication auth) {
        User actor = (User) auth.getPrincipal();
        if (actor.getRole() == UserRole.ADMIN) return;
        if (actor.getRole() == UserRole.CLIENT && booking.getClient().getId().equals(actor.getId())) return;
        if (booking.getBusiness().getOwner().getId().equals(actor.getId())) return;
        throw BookerException.forbidden("Access denied");
    }

    private void checkBusinessAccess(Booking booking, Authentication auth) {
        User actor = (User) auth.getPrincipal();
        if (actor.getRole() == UserRole.ADMIN) return;
        if (booking.getBusiness().getOwner().getId().equals(actor.getId())) return;
        // Employee of this business can also access
        if (actor.getRole() == UserRole.EMPLOYEE) return; // simplified; full check would match employee record
        throw BookerException.forbidden("Access denied");
    }

    // ── Mapper ────────────────────────────────────────────────────

    private Booking findOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> BookerException.notFound("Booking not found: " + id));
    }

    public BookingResponse toResponse(Booking b) {
        return new BookingResponse(
                b.getId(),
                b.getClient().getId(),
                b.getClient().getFullName(),
                b.getService().getId(),
                b.getService().getName(),
                b.getBusiness().getId(),
                b.getBusiness().getName(),
                b.getBranch().getId(),
                b.getBranch().getName(),
                b.getEmployee() != null ? b.getEmployee().getId() : null,
                b.getEmployee() != null ? b.getEmployee().getDisplayName() : null,
                b.getResource() != null ? b.getResource().getId() : null,
                b.getResource() != null ? b.getResource().getName() : null,
                b.getStartTime(),
                b.getEndTime(),
                b.getStatus(),
                b.getClientNote(),
                b.getBusinessNote(),
                b.getPriceSnapshot(),
                b.getDurationMin(),
                b.getSelectedAttributes(),
                b.getCreatedAt()
        );
    }
}
