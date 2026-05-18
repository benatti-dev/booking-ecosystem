package com.booker.booking.controller;

import com.booker.auth.entity.User;
import com.booker.booking.dto.BookingResponse;
import com.booker.booking.dto.CancelBookingRequest;
import com.booker.booking.dto.CreateBookingRequest;
import com.booker.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking management")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Create a new booking")
    public BookingResponse create(
            @Valid @RequestBody CreateBookingRequest req,
            Authentication auth) {
        User client = (User) auth.getPrincipal();
        return bookingService.createBooking(req, client);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking by ID")
    public BookingResponse getById(@PathVariable Long id, Authentication auth) {
        return bookingService.getById(id, auth);
    }

    @GetMapping("/my")
    @Operation(summary = "Get current client's booking history")
    public Page<BookingResponse> getMyBookings(Authentication auth, Pageable pageable) {
        User client = (User) auth.getPrincipal();
        return bookingService.getMyBookings(client.getId(), pageable);
    }

    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','EMPLOYEE','ADMIN')")
    @Operation(summary = "Confirm a pending booking")
    public BookingResponse confirm(@PathVariable Long id, Authentication auth) {
        return bookingService.confirmBooking(id, auth);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a booking")
    public BookingResponse cancel(
            @PathVariable Long id,
            @RequestBody(required = false) CancelBookingRequest req,
            Authentication auth) {
        return bookingService.cancelBooking(id, req, auth);
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','EMPLOYEE','ADMIN')")
    @Operation(summary = "Mark booking as completed")
    public BookingResponse complete(@PathVariable Long id, Authentication auth) {
        return bookingService.completeBooking(id, auth);
    }

    @GetMapping("/business/{businessId}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','EMPLOYEE','ADMIN')")
    @Operation(summary = "Get bookings for a business (with optional date range)")
    public Page<BookingResponse> getBusinessBookings(
            @PathVariable Long businessId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            Pageable pageable,
            Authentication auth) {
        return bookingService.getBusinessBookings(businessId, from, to, pageable);
    }
}
