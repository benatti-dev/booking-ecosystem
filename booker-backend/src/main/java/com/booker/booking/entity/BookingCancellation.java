package com.booker.booking.entity;

import com.booker.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "booking_cancellations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCancellation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cancelled_by", nullable = false)
    private User cancelledBy;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "cancelled_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant cancelledAt = Instant.now();
}
