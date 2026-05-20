package com.booker.booking;

import com.booker.auth.entity.User;
import com.booker.auth.entity.UserRole;
import com.booker.booking.dto.BookingResponse;
import com.booker.booking.dto.CancelBookingRequest;
import com.booker.booking.entity.Booking;
import com.booker.booking.entity.BookingStatus;
import com.booker.booking.event.BookingCancelledEvent;
import com.booker.booking.event.BookingCompletedEvent;
import com.booker.booking.event.BookingConfirmedEvent;
import com.booker.booking.repository.BookingCancellationRepository;
import com.booker.booking.repository.BookingRepository;
import com.booker.booking.service.BookingService;
import com.booker.booking.service.BookingValidator;
import com.booker.business.entity.Branch;
import com.booker.business.entity.Business;
import com.booker.business.entity.BusinessStatus;
import com.booker.business.repository.BranchRepository;
import com.booker.business.repository.BusinessRepository;
import com.booker.business.repository.EmployeeRepository;
import com.booker.business.repository.ResourceRepository;
import com.booker.catalog.entity.Service;
import com.booker.shared.exception.BookerException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BookingService} status-transition methods.
 *
 * The createBooking flow involves PostgreSQL advisory locks via EntityManager
 * and is better tested at the integration level (BookingIntegrationTest /
 * BookingConcurrencyIntegrationTest). These tests cover confirm / complete /
 * cancel logic which is straightforward to unit-test with mocked repositories.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService — Unit Tests")
class BookingServiceUnitTest {

    @Mock BookingRepository             bookingRepository;
    @Mock BookingCancellationRepository cancellationRepository;
    @Mock BookingValidator              bookingValidator;
    @Mock BranchRepository             branchRepository;
    @Mock BusinessRepository           businessRepository;
    @Mock EmployeeRepository           employeeRepository;
    @Mock ResourceRepository           resourceRepository;
    @Mock ApplicationEventPublisher    eventPublisher;
    @Mock EntityManager                entityManager;

    @InjectMocks BookingService bookingService;

    private User     owner;
    private User     client;
    private Business business;
    private Branch   branch;
    private Service  service;
    private Booking  pendingBooking;
    private Booking  confirmedBooking;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(2L).email("owner@test.com").fullName("Business Owner")
                .role(UserRole.BUSINESS_OWNER).build();

        client = User.builder()
                .id(1L).email("client@test.com").fullName("Test Client")
                .role(UserRole.CLIENT).build();

        business = Business.builder()
                .id(10L).name("Test Business").status(BusinessStatus.ACTIVE)
                .owner(owner).build();

        branch = Branch.builder().id(30L).business(business).name("Main Branch").build();

        service = Service.builder()
                .id(20L).name("Haircut").durationMin(60)
                .price(BigDecimal.valueOf(500)).isActive(true)
                .business(business).build();

        pendingBooking = Booking.builder()
                .id(100L).client(client).service(service).business(business).branch(branch)
                .startTime(Instant.now().plus(2, ChronoUnit.DAYS))
                .endTime(Instant.now().plus(2, ChronoUnit.DAYS).plus(60, ChronoUnit.MINUTES))
                .status(BookingStatus.PENDING)
                .priceSnapshot(BigDecimal.valueOf(500)).durationMin(60)
                .build();

        confirmedBooking = Booking.builder()
                .id(101L).client(client).service(service).business(business).branch(branch)
                .startTime(Instant.now().plus(2, ChronoUnit.DAYS))
                .endTime(Instant.now().plus(2, ChronoUnit.DAYS).plus(60, ChronoUnit.MINUTES))
                .status(BookingStatus.CONFIRMED)
                .priceSnapshot(BigDecimal.valueOf(500)).durationMin(60)
                .build();
    }

    // ── confirmBooking ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("confirmBooking")
    class ConfirmBooking {

        @Test
        @DisplayName("PENDING → CONFIRMED by owner → status updated and event published")
        void confirm_pendingByOwner_success() {
            when(bookingRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(pendingBooking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

            Authentication auth = mockAuth(owner);
            BookingResponse response = bookingService.confirmBooking(100L, auth);

            assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
            ArgumentCaptor<BookingConfirmedEvent> captor = ArgumentCaptor.forClass(BookingConfirmedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().clientEmail()).isEqualTo("client@test.com");
        }

        @Test
        @DisplayName("already CONFIRMED → throws 400")
        void confirm_alreadyConfirmed_throws() {
            when(bookingRepository.findByIdWithDetails(101L)).thenReturn(Optional.of(confirmedBooking));
            Authentication auth = mockAuth(owner);

            assertThatThrownBy(() -> bookingService.confirmBooking(101L, auth))
                    .isInstanceOf(BookerException.class)
                    .hasMessageContaining("PENDING");
        }

        @Test
        @DisplayName("CLIENT tries to confirm → throws 403")
        void confirm_byClient_throws() {
            when(bookingRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(pendingBooking));
            Authentication auth = mockAuth(client);

            assertThatThrownBy(() -> bookingService.confirmBooking(100L, auth))
                    .isInstanceOf(BookerException.class);
        }
    }

    // ── completeBooking ────────────────────────────────────────────────────

    @Nested
    @DisplayName("completeBooking")
    class CompleteBooking {

        @Test
        @DisplayName("CONFIRMED → COMPLETED by owner → status updated and event published")
        void complete_confirmedByOwner_success() {
            when(bookingRepository.findByIdWithDetails(101L)).thenReturn(Optional.of(confirmedBooking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

            Authentication auth = mockAuth(owner);
            BookingResponse response = bookingService.completeBooking(101L, auth);

            assertThat(response.status()).isEqualTo(BookingStatus.COMPLETED);
            verify(eventPublisher).publishEvent(any(BookingCompletedEvent.class));
        }

        @Test
        @DisplayName("PENDING (not confirmed) → throws 400")
        void complete_pending_throws() {
            when(bookingRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(pendingBooking));
            Authentication auth = mockAuth(owner);

            assertThatThrownBy(() -> bookingService.completeBooking(100L, auth))
                    .isInstanceOf(BookerException.class)
                    .hasMessageContaining("CONFIRMED");
        }
    }

    // ── cancelBooking ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelBooking")
    class CancelBooking {

        @Test
        @DisplayName("owner cancels PENDING booking → CANCELLED and event published")
        void cancel_byOwner_success() {
            when(bookingRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(pendingBooking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
            when(cancellationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Authentication auth = mockAuth(owner);
            BookingResponse response = bookingService.cancelBooking(100L, new CancelBookingRequest("No show"), auth);

            assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
            verify(eventPublisher).publishEvent(any(BookingCancelledEvent.class));
        }

        @Test
        @DisplayName("client cancels own booking with >24h notice → CANCELLED")
        void cancel_byClientWithEnoughNotice_success() {
            when(bookingRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(pendingBooking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
            when(cancellationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Authentication auth = mockAuth(client);
            BookingResponse response = bookingService.cancelBooking(
                    100L, new CancelBookingRequest("Changed mind"), auth);

            assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
        }

        @Test
        @DisplayName("client cancels with <24h notice → throws 400")
        void cancel_byClientTooLate_throws() {
            Booking soonBooking = Booking.builder()
                    .id(102L).client(client).service(service).business(business).branch(branch)
                    .startTime(Instant.now().plus(2, ChronoUnit.HOURS))
                    .endTime(Instant.now().plus(3, ChronoUnit.HOURS))
                    .status(BookingStatus.PENDING)
                    .priceSnapshot(BigDecimal.valueOf(500)).durationMin(60)
                    .build();

            when(bookingRepository.findByIdWithDetails(102L)).thenReturn(Optional.of(soonBooking));
            Authentication auth = mockAuth(client);

            assertThatThrownBy(() -> bookingService.cancelBooking(
                    102L, new CancelBookingRequest("Late cancel"), auth))
                    .isInstanceOf(BookerException.class)
                    .hasMessageContaining("24 hours");
        }

        @Test
        @DisplayName("client cancels another client's booking → throws 403")
        void cancel_wrongClient_throws() {
            User otherClient = User.builder().id(99L).email("other@test.com")
                    .fullName("Other").role(UserRole.CLIENT).build();

            when(bookingRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(pendingBooking));
            Authentication auth = mockAuth(otherClient);

            assertThatThrownBy(() -> bookingService.cancelBooking(
                    100L, new CancelBookingRequest("x"), auth))
                    .isInstanceOf(BookerException.class)
                    .hasMessageContaining("own bookings");
        }

        @Test
        @DisplayName("cancel COMPLETED booking → throws 400")
        void cancel_completedBooking_throws() {
            Booking completed = Booking.builder()
                    .id(103L).client(client).service(service).business(business).branch(branch)
                    .startTime(Instant.now().minus(1, ChronoUnit.DAYS))
                    .endTime(Instant.now().minus(1, ChronoUnit.DAYS).plus(60, ChronoUnit.MINUTES))
                    .status(BookingStatus.COMPLETED)
                    .priceSnapshot(BigDecimal.valueOf(500)).durationMin(60)
                    .build();

            when(bookingRepository.findByIdWithDetails(103L)).thenReturn(Optional.of(completed));
            Authentication auth = mockAuth(owner);

            assertThatThrownBy(() -> bookingService.cancelBooking(
                    103L, new CancelBookingRequest("Oops"), auth))
                    .isInstanceOf(BookerException.class)
                    .hasMessageContaining("COMPLETED");
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private Authentication mockAuth(User user) {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        return auth;
    }
}
