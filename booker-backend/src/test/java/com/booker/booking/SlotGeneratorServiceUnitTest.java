package com.booker.booking;

import com.booker.booking.entity.*;
import com.booker.booking.repository.*;
import com.booker.booking.service.SlotGeneratorService;
import com.booker.catalog.entity.Service;
import com.booker.catalog.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link SlotGeneratorService}.
 * All repository calls are mocked so no database is required.
 *
 * The slot algorithm contract:
 *  - Slots are generated from workStart to workEnd in SLOT_STEP_MINUTES (15) increments.
 *  - A slot is only available if [slotStart, slotStart+duration) does not overlap any break
 *    or any existing booking.
 *  - An override with null start/end marks a full-day off → no slots.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SlotGeneratorService — Unit Tests")
class SlotGeneratorServiceUnitTest {

    @Mock ScheduleRuleRepository     ruleRepository;
    @Mock ScheduleOverrideRepository overrideRepository;
    @Mock ScheduleBreakRepository    breakRepository;
    @Mock BookingRepository          bookingRepository;
    @Mock ServiceRepository          serviceRepository;

    @InjectMocks SlotGeneratorService slotGeneratorService;

    /** Fixed Monday in a far-future week to avoid day-of-week flakiness. */
    private static final LocalDate   MONDAY   = LocalDate.of(2030, 1, 7);
    private static final String      TIMEZONE = "Europe/Kyiv";
    private static final ZoneId      ZONE     = ZoneId.of(TIMEZONE);
    private static final long        EMP_ID   = 1L;
    private static final long        SVC_ID   = 10L;

    private Service service60;
    private ScheduleRule mondayRule;

    @BeforeEach
    void setUp() {
        service60 = Service.builder()
                .durationMin(60)
                .name("Haircut")
                .isActive(true)
                .price(BigDecimal.valueOf(500))
                .build();

        // Monday is day 1 in the DB convention (1=Mon…7=Sun)
        mondayRule = ScheduleRule.builder()
                .id(100L)
                .dayOfWeek((short) 1)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))   // 3-hour window → 9 slots (60 min, 15-min step)
                .isWorkingDay(true)
                .build();
    }

    // ── Happy path ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("No conflicts")
    class NoConflicts {

        @Test
        @DisplayName("returns all valid slots within working hours")
        void allSlotsAvailable() {
            given_rule_override_breaks_bookings(mondayRule, Optional.empty(), List.of(), List.of());

            List<LocalTime> slots = slotGeneratorService.getAvailableSlotsForEmployee(
                    EMP_ID, SVC_ID, MONDAY, TIMEZONE);

            // 09:00 → 11:00 in 15-min steps: 9:00, 9:15, 9:30, 9:45, 10:00, 10:15, 10:30, 10:45, 11:00
            assertThat(slots).hasSize(9);
            assertThat(slots).first().isEqualTo(LocalTime.of(9, 0));
            assertThat(slots).last().isEqualTo(LocalTime.of(11, 0));
        }
    }

    // ── Non-working day ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Non-working day")
    class NonWorkingDay {

        @Test
        @DisplayName("no schedule rule for day → empty")
        void noRuleForDay() {
            when(serviceRepository.findById(SVC_ID)).thenReturn(Optional.of(service60));
            when(ruleRepository.findByEmployeeIdAndDayOfWeek(EMP_ID, 1))
                    .thenReturn(Optional.empty());

            assertThat(slotGeneratorService.getAvailableSlotsForEmployee(
                    EMP_ID, SVC_ID, MONDAY, TIMEZONE)).isEmpty();
        }

        @Test
        @DisplayName("rule marks day as non-working → empty")
        void ruleIsNonWorkingDay() {
            ScheduleRule offRule = ScheduleRule.builder()
                    .id(101L).dayOfWeek((short) 1)
                    .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(18, 0))
                    .isWorkingDay(false).build();

            when(serviceRepository.findById(SVC_ID)).thenReturn(Optional.of(service60));
            when(ruleRepository.findByEmployeeIdAndDayOfWeek(EMP_ID, 1))
                    .thenReturn(Optional.of(offRule));

            assertThat(slotGeneratorService.getAvailableSlotsForEmployee(
                    EMP_ID, SVC_ID, MONDAY, TIMEZONE)).isEmpty();
        }
    }

    // ── Schedule overrides ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Schedule overrides")
    class ScheduleOverrides {

        @Test
        @DisplayName("full-day override (null start/end) → empty")
        void fullDayOff() {
            ScheduleOverride dayOff = ScheduleOverride.builder()
                    .startTime(null).endTime(null).build();

            given_rule_override_breaks_bookings(mondayRule, Optional.of(dayOff), List.of(), List.of());

            assertThat(slotGeneratorService.getAvailableSlotsForEmployee(
                    EMP_ID, SVC_ID, MONDAY, TIMEZONE)).isEmpty();
        }

        @Test
        @DisplayName("override shifts working hours to afternoon")
        void overrideShiftsHours() {
            ScheduleOverride afternoonShift = ScheduleOverride.builder()
                    .startTime(LocalTime.of(14, 0))
                    .endTime(LocalTime.of(15, 0)) // 1-hour window → only 09:00 slot fits 60-min service
                    .build();

            given_rule_override_breaks_bookings(mondayRule, Optional.of(afternoonShift), List.of(), List.of());

            List<LocalTime> slots = slotGeneratorService.getAvailableSlotsForEmployee(
                    EMP_ID, SVC_ID, MONDAY, TIMEZONE);

            assertThat(slots).hasSize(1);
            assertThat(slots).containsExactly(LocalTime.of(14, 0));
        }
    }

    // ── Breaks ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Schedule breaks")
    class Breaks {

        @Test
        @DisplayName("break at 10:00–10:30 blocks two slots")
        void breakBlocksSlots() {
            ScheduleBreak lunchBreak = ScheduleBreak.builder()
                    .startTime(LocalTime.of(10, 0))
                    .endTime(LocalTime.of(10, 30))
                    .build();

            given_rule_override_breaks_bookings(mondayRule, Optional.empty(), List.of(lunchBreak), List.of());

            List<LocalTime> slots = slotGeneratorService.getAvailableSlotsForEmployee(
                    EMP_ID, SVC_ID, MONDAY, TIMEZONE);

            // 9:00 and 9:15 overlap with break (60-min service runs into 10:00–10:30).
            // Only 09:00 starts exactly at start and ends at 10:00 → does NOT overlap 10:00–10:30
            // because overlap is slotEnd > breakStart (10:00 > 10:00 is false).
            // 9:15 slot would run 9:15–10:15 → 10:15 > 10:00 ✓ AND 9:15 < 10:30 ✓ → overlaps.
            // Slots clear of the break: 9:00 and 10:30 (if 10:30+60 ≤ 12:00 → 10:30 ok, 10:45 ok, 11:00 ok)
            assertThat(slots).doesNotContain(LocalTime.of(9, 15));
            assertThat(slots).doesNotContain(LocalTime.of(9, 30));
            assertThat(slots).contains(LocalTime.of(9, 0));
            assertThat(slots).contains(LocalTime.of(10, 30));
        }

        @Test
        @DisplayName("break spanning entire working window → empty")
        void breakCoversAll() {
            ScheduleBreak fullBreak = ScheduleBreak.builder()
                    .startTime(LocalTime.of(8, 0))
                    .endTime(LocalTime.of(13, 0))
                    .build();

            given_rule_override_breaks_bookings(mondayRule, Optional.empty(), List.of(fullBreak), List.of());

            assertThat(slotGeneratorService.getAvailableSlotsForEmployee(
                    EMP_ID, SVC_ID, MONDAY, TIMEZONE)).isEmpty();
        }
    }

    // ── Existing bookings ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Existing bookings conflict")
    class ExistingBookings {

        @Test
        @DisplayName("booking at 09:00–10:00 blocks overlapping slots")
        void existingBookingBlocksSlots() {
            Booking existingBooking = bookingAt(LocalTime.of(9, 0), LocalTime.of(10, 0));
            given_rule_override_breaks_bookings(mondayRule, Optional.empty(), List.of(), List.of(existingBooking));

            List<LocalTime> slots = slotGeneratorService.getAvailableSlotsForEmployee(
                    EMP_ID, SVC_ID, MONDAY, TIMEZONE);

            // Slots starting at 9:00 end at 10:00 — 10:00 > 9:00 ✓ AND 9:00 < 10:00 ✓ → overlaps.
            // Also 9:15→10:15 overlaps.
            assertThat(slots).doesNotContain(LocalTime.of(9, 0));
            assertThat(slots).doesNotContain(LocalTime.of(9, 15));
            assertThat(slots).contains(LocalTime.of(10, 0));
        }

        @Test
        @DisplayName("two back-to-back bookings leave a gap for one slot")
        void twoBookingsLeaveSingleGap() {
            Booking first  = bookingAt(LocalTime.of(9, 0), LocalTime.of(10, 0));
            Booking second = bookingAt(LocalTime.of(10, 0), LocalTime.of(11, 0));
            given_rule_override_breaks_bookings(mondayRule, Optional.empty(), List.of(), List.of(first, second));

            List<LocalTime> slots = slotGeneratorService.getAvailableSlotsForEmployee(
                    EMP_ID, SVC_ID, MONDAY, TIMEZONE);

            // Only 11:00 fits: 11:00+60 = 12:00 ≤ 12:00 ✓, no overlap
            assertThat(slots).containsExactly(LocalTime.of(11, 0));
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void given_rule_override_breaks_bookings(
            ScheduleRule rule,
            Optional<ScheduleOverride> override,
            List<ScheduleBreak> breaks,
            List<Booking> bookings) {

        when(serviceRepository.findById(SVC_ID)).thenReturn(Optional.of(service60));
        when(ruleRepository.findByEmployeeIdAndDayOfWeek(EMP_ID, 1)).thenReturn(Optional.of(rule));
        when(overrideRepository.findByEmployeeIdAndDate(EMP_ID, MONDAY)).thenReturn(override);
        when(breakRepository.findByScheduleRuleId(rule.getId())).thenReturn(breaks);

        Instant dayStart = MONDAY.atStartOfDay(ZONE).toInstant();
        Instant dayEnd   = MONDAY.plusDays(1).atStartOfDay(ZONE).toInstant();
        when(bookingRepository.findActiveByEmployeeAndDateRange(EMP_ID, dayStart, dayEnd))
                .thenReturn(bookings);
    }

    private Booking bookingAt(LocalTime start, LocalTime end) {
        return Booking.builder()
                .startTime(MONDAY.atTime(start).atZone(ZONE).toInstant())
                .endTime(MONDAY.atTime(end).atZone(ZONE).toInstant())
                .status(BookingStatus.CONFIRMED)
                .build();
    }
}
