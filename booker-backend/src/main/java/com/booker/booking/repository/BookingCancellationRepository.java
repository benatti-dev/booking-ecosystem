package com.booker.booking.repository;

import com.booker.booking.entity.BookingCancellation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingCancellationRepository extends JpaRepository<BookingCancellation, Long> {
}
