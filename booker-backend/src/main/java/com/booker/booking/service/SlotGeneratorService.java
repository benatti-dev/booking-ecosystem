package com.booker.booking.service;

import com.booker.booking.entity.Booking;
import com.booker.booking.entity.ScheduleBreak;
import com.booker.booking.entity.ScheduleOverride;
import com.booker.booking.entity.ScheduleRule;
import com.booker.booking.repository.BookingRepository;
import com.booker.booking.repository.ScheduleBreakRepository;
import com.booker.booking.repository.ScheduleOverrideRepository;
import com.booker.booking.repository.ScheduleRuleRepository;
import com.booker.catalog.entity.Service;
import com.booker.catalog.repository.ServiceRepository;
import com.booker.shared.exception.BookerException;
import lombok.RequiredArgsConstructor;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Pure computation — generates available booking slots for a given date.
 *  No DB writes, fully unit-testable. */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class SlotGeneratorService {

    private static final int SLOT_STEP_MINUTES = 15;

    private final ScheduleRuleRepository ruleRepository;
    private final ScheduleOverrideRepository overrideRepository;
    private final ScheduleBreakRepository breakRepository;
    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;

    /**
     * Returns available start times for an employee on a given date.
     *
     * @param employeeId target employee
     * @param serviceId  service to book (determines duration)
     * @param date       target date (UTC)
     * @param timezone   business timezone (e.g. "Europe/Kiev")
     */
    public List<LocalTime> getAvailableSlotsForEmployee(Long employeeId, Long serviceId,
                                                         LocalDate date, String timezone) {
        Service service = findServiceOrThrow(serviceId);
        int durationMin = service.getDurationMin();

        // Step 1: Load schedule rule for this day
        int dayOfWeek = date.getDayOfWeek().getValue() % 7; // 0=Sunday … 6=Saturday
        Optional<ScheduleRule> ruleOpt = ruleRepository.findByEmployeeIdAndDayOfWeek(employeeId, dayOfWeek);
        if (ruleOpt.isEmpty() || !ruleOpt.get().isWorkingDay()) {
            return List.of();
        }
        ScheduleRule rule = ruleOpt.get();

        // Step 2: Apply schedule override for this date
        Optional<ScheduleOverride> overrideOpt = overrideRepository.findByEmployeeIdAndDate(employeeId, date);
        LocalTime workStart = rule.getStartTime();
        LocalTime workEnd = rule.getEndTime();

        if (overrideOpt.isPresent()) {
            ScheduleOverride override = overrideOpt.get();
            if (override.getStartTime() == null && override.getEndTime() == null) {
                return List.of(); // full-day off
            }
            if (override.getStartTime() != null) workStart = override.getStartTime();
            if (override.getEndTime() != null) workEnd = override.getEndTime();
        }

        // Step 3: Load breaks
        List<ScheduleBreak> breaks = breakRepository.findByScheduleRuleId(rule.getId());

        // Step 4: Load existing bookings for this employee on this date
        ZoneId zone = ZoneId.of(timezone);
        Instant dayStart = date.atStartOfDay(zone).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();
        List<Booking> existingBookings = bookingRepository.findActiveByEmployeeAndDateRange(employeeId, dayStart, dayEnd);

        // Step 5: Generate slots
        return computeSlots(workStart, workEnd, breaks, existingBookings, durationMin, date, zone);
    }

    /**
     * Returns available start times for a bookable resource on a given date.
     */
    public List<LocalTime> getAvailableSlotsForResource(Long resourceId, Long serviceId,
                                                          LocalDate date, String timezone) {
        Service service = findServiceOrThrow(serviceId);
        int durationMin = service.getDurationMin();

        int dayOfWeek = date.getDayOfWeek().getValue() % 7;
        Optional<ScheduleRule> ruleOpt = ruleRepository.findByResourceIdAndDayOfWeek(resourceId, dayOfWeek);
        if (ruleOpt.isEmpty() || !ruleOpt.get().isWorkingDay()) {
            return List.of();
        }
        ScheduleRule rule = ruleOpt.get();

        Optional<ScheduleOverride> overrideOpt = overrideRepository.findByResourceIdAndDate(resourceId, date);
        LocalTime workStart = rule.getStartTime();
        LocalTime workEnd = rule.getEndTime();

        if (overrideOpt.isPresent()) {
            ScheduleOverride override = overrideOpt.get();
            if (override.getStartTime() == null && override.getEndTime() == null) {
                return List.of();
            }
            if (override.getStartTime() != null) workStart = override.getStartTime();
            if (override.getEndTime() != null) workEnd = override.getEndTime();
        }

        List<ScheduleBreak> breaks = breakRepository.findByScheduleRuleId(rule.getId());

        ZoneId zone = ZoneId.of(timezone);
        Instant dayStart = date.atStartOfDay(zone).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();
        List<Booking> existingBookings = bookingRepository.findActiveByResourceAndDateRange(resourceId, dayStart, dayEnd);

        return computeSlots(workStart, workEnd, breaks, existingBookings, durationMin, date, zone);
    }

    // ── Core algorithm ────────────────────────────────────────────

    private List<LocalTime> computeSlots(LocalTime workStart, LocalTime workEnd,
                                          List<ScheduleBreak> breaks, List<Booking> existingBookings,
                                          int durationMin, LocalDate date, ZoneId zone) {
        List<LocalTime> available = new ArrayList<>();
        LocalTime cursor = workStart;

        while (!cursor.plusMinutes(durationMin).isAfter(workEnd)) {
            LocalTime slotEnd = cursor.plusMinutes(durationMin);

            if (!overlapsBreak(cursor, slotEnd, breaks)
                    && !overlapsBooking(cursor, slotEnd, existingBookings, date, zone)) {
                available.add(cursor);
            }

            cursor = cursor.plusMinutes(SLOT_STEP_MINUTES);
        }

        return available;
    }

    private boolean overlapsBreak(LocalTime slotStart, LocalTime slotEnd, List<ScheduleBreak> breaks) {
        for (ScheduleBreak b : breaks) {
            // Overlap: slotStart < breakEnd AND slotEnd > breakStart
            if (slotStart.isBefore(b.getEndTime()) && slotEnd.isAfter(b.getStartTime())) {
                return true;
            }
        }
        return false;
    }

    private boolean overlapsBooking(LocalTime slotStart, LocalTime slotEnd,
                                    List<Booking> bookings, LocalDate date, ZoneId zone) {
        Instant slotStartInstant = date.atTime(slotStart).atZone(zone).toInstant();
        Instant slotEndInstant = date.atTime(slotEnd).atZone(zone).toInstant();

        for (Booking b : bookings) {
            // Overlap: slotStart < bookingEnd AND slotEnd > bookingStart
            if (slotStartInstant.isBefore(b.getEndTime()) && slotEndInstant.isAfter(b.getStartTime())) {
                return true;
            }
        }
        return false;
    }

    private Service findServiceOrThrow(Long serviceId) {
        return serviceRepository.findById(serviceId)
                .orElseThrow(() -> BookerException.notFound("Service not found: " + serviceId));
    }
}
