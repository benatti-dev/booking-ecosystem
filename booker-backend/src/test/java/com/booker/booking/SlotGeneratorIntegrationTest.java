package com.booker.booking;

import com.booker.auth.entity.User;
import com.booker.auth.entity.UserRole;
import com.booker.auth.repository.UserRepository;
import com.booker.booking.entity.*;
import com.booker.booking.repository.*;
import com.booker.booking.service.SlotGeneratorService;
import com.booker.business.entity.*;
import com.booker.business.repository.*;
import com.booker.catalog.entity.Service;
import com.booker.catalog.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@DisplayName("SlotGeneratorService Integration Tests")
class SlotGeneratorIntegrationTest {

    @Autowired SlotGeneratorService slotGeneratorService;
    @Autowired UserRepository userRepository;
    @Autowired BusinessCategoryRepository categoryRepository;
    @Autowired BusinessRepository businessRepository;
    @Autowired BranchRepository branchRepository;
    @Autowired ServiceRepository serviceRepository;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired ScheduleRuleRepository scheduleRuleRepository;
    @Autowired ScheduleBreakRepository scheduleBreakRepository;
    @Autowired ScheduleOverrideRepository scheduleOverrideRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired BookingCancellationRepository cancellationRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private static final String TZ = "Europe/Kiev";

    private Employee employee;
    private Service service60;   // 60-minute service
    private Service service30;   // 30-minute service
    private Branch branch;
    private Business business;
    private BusinessCategory category;
    private User owner;

    /** Next Monday from today */
    private static final LocalDate MONDAY = LocalDate.now().with(
            java.time.temporal.TemporalAdjusters.next(DayOfWeek.MONDAY));

    @BeforeEach
    void setup() {
        cancellationRepository.deleteAll();
        bookingRepository.deleteAll();
        scheduleOverrideRepository.deleteAll();
        scheduleBreakRepository.deleteAll();
        scheduleRuleRepository.deleteAll();
        employeeRepository.deleteAll();
        serviceRepository.deleteAll();
        branchRepository.deleteAll();
        businessRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(User.builder()
                .email("owner@slot.test").passwordHash(passwordEncoder.encode("pass"))
                .fullName("Owner").role(UserRole.BUSINESS_OWNER).build());

        category = categoryRepository.save(BusinessCategory.builder()
                .name("beauty").label("Beauty").resourceType(ResourceType.EMPLOYEE).build());

        business = businessRepository.save(Business.builder()
                .owner(owner).category(category).name("Slot Test Salon")
                .status(BusinessStatus.ACTIVE).build());

        branch = branchRepository.save(Branch.builder()
                .business(business).name("Main").address("1 Street")
                .city("Kyiv").timezone(TZ).build());

        service60 = serviceRepository.save(Service.builder()
                .business(business).category(category)
                .name("60min Service").durationMin(60)
                .price(BigDecimal.valueOf(300)).isActive(true).build());

        service30 = serviceRepository.save(Service.builder()
                .business(business).category(category)
                .name("30min Service").durationMin(30)
                .price(BigDecimal.valueOf(150)).isActive(true).build());

        employee = employeeRepository.save(Employee.builder()
                .business(business).branch(branch)
                .displayName("Test Employee").isActive(true).build());
    }

    // ── Working day ───────────────────────────────────────────────

    @Test @DisplayName("Working day 09:00–18:00 with 60-min service → first slot 09:00")
    void workingDay_firstSlotIsWorkStart() {
        addRule(MONDAY.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.of(18, 0), true);

        List<LocalTime> slots = slotGeneratorService
                .getAvailableSlotsForEmployee(employee.getId(), service60.getId(), MONDAY, TZ);

        assertThat(slots).isNotEmpty();
        assertThat(slots.getFirst()).isEqualTo(LocalTime.of(9, 0));
    }

    @Test @DisplayName("Working day 09:00–10:00 with 60-min service → exactly one slot")
    void workingDay_exactlyOneSlot() {
        addRule(MONDAY.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.of(10, 0), true);

        List<LocalTime> slots = slotGeneratorService
                .getAvailableSlotsForEmployee(employee.getId(), service60.getId(), MONDAY, TZ);

        assertThat(slots).containsExactly(LocalTime.of(9, 0));
    }

    @Test @DisplayName("Working day 09:00–18:00 with 30-min service → correct slot count")
    void workingDay_30minService_correctSlotCount() {
        addRule(MONDAY.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.of(18, 0), true);

        List<LocalTime> slots = slotGeneratorService
                .getAvailableSlotsForEmployee(employee.getId(), service30.getId(), MONDAY, TZ);

        // 09:00–18:00 = 540 min window, 30-min service, 15-min step
        // Slots: 09:00, 09:15, ..., 17:30 → (540-30)/15 + 1 = 35 slots
        assertThat(slots).hasSize(35);
        assertThat(slots.getLast()).isEqualTo(LocalTime.of(17, 30));
    }

    // ── Non-working day ───────────────────────────────────────────

    @Test @DisplayName("Non-working day → empty slots")
    void nonWorkingDay_empty() {
        addRule(MONDAY.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.of(18, 0), false);

        List<LocalTime> slots = slotGeneratorService
                .getAvailableSlotsForEmployee(employee.getId(), service60.getId(), MONDAY, TZ);

        assertThat(slots).isEmpty();
    }

    @Test @DisplayName("No schedule rule for this day → empty slots")
    void noRule_empty() {
        // No rule saved → empty
        List<LocalTime> slots = slotGeneratorService
                .getAvailableSlotsForEmployee(employee.getId(), service60.getId(), MONDAY, TZ);

        assertThat(slots).isEmpty();
    }

    // ── Full-day override ─────────────────────────────────────────

    @Test @DisplayName("Full-day override (startTime=null) → empty slots")
    void fullDayOverride_empty() {
        addRule(MONDAY.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.of(18, 0), true);
        scheduleOverrideRepository.save(ScheduleOverride.builder()
                .employee(employee).overrideDate(MONDAY)
                .startTime(null).endTime(null)
                .reason("Public holiday").build());

        List<LocalTime> slots = slotGeneratorService
                .getAvailableSlotsForEmployee(employee.getId(), service60.getId(), MONDAY, TZ);

        assertThat(slots).isEmpty();
    }

    @Test @DisplayName("Partial override shifts working window → slots reflect new times")
    void partialOverride_shiftsWindow() {
        addRule(MONDAY.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.of(18, 0), true);
        // Override: work only 14:00–16:00
        scheduleOverrideRepository.save(ScheduleOverride.builder()
                .employee(employee).overrideDate(MONDAY)
                .startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(16, 0))
                .reason("Shortened day").build());

        List<LocalTime> slots = slotGeneratorService
                .getAvailableSlotsForEmployee(employee.getId(), service60.getId(), MONDAY, TZ);

        // 14:00–16:00 with 60-min service → only 14:00 fits
        assertThat(slots).containsExactly(LocalTime.of(14, 0));
    }

    // ── Breaks ────────────────────────────────────────────────────

    @Test @DisplayName("Break 12:00–13:00 blocks slots overlapping lunch")
    void break_blocksOverlappingSlots() {
        ScheduleRule rule = addRule(MONDAY.getDayOfWeek(),
                LocalTime.of(9, 0), LocalTime.of(18, 0), true);

        scheduleBreakRepository.save(ScheduleBreak.builder()
                .scheduleRule(rule)
                .startTime(LocalTime.of(12, 0))
                .endTime(LocalTime.of(13, 0))
                .build());

        List<LocalTime> slots = slotGeneratorService
                .getAvailableSlotsForEmployee(employee.getId(), service60.getId(), MONDAY, TZ);

        // Slots that overlap 12:00–13:00 must be absent
        assertThat(slots).doesNotContain(LocalTime.of(11, 30)); // would end at 12:30 — overlaps
        assertThat(slots).doesNotContain(LocalTime.of(12, 0));  // starts during break
        assertThat(slots).contains(LocalTime.of(11, 0));        // ends at 12:00 — OK
        assertThat(slots).contains(LocalTime.of(13, 0));        // starts after break — OK
    }

    // ── Existing bookings ─────────────────────────────────────────

    @Test @DisplayName("Existing booking blocks overlapping slots")
    void existingBooking_blocksSlot() {
        addRule(MONDAY.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.of(18, 0), true);

        // Book 10:00–11:00
        ZoneId zone = ZoneId.of(TZ);
        Instant start = MONDAY.atTime(10, 0).atZone(zone).toInstant();
        Instant end   = MONDAY.atTime(11, 0).atZone(zone).toInstant();

        User client = userRepository.save(User.builder()
                .email("client@slot.test").passwordHash(passwordEncoder.encode("p"))
                .fullName("Client").role(UserRole.CLIENT).build());

        bookingRepository.save(Booking.builder()
                .client(client).service(service60).business(business).branch(branch)
                .employee(employee).startTime(start).endTime(end)
                .status(BookingStatus.CONFIRMED)
                .durationMin(60).priceSnapshot(service60.getPrice())
                .selectedAttributes(new java.util.HashMap<>()).build());

        List<LocalTime> slots = slotGeneratorService
                .getAvailableSlotsForEmployee(employee.getId(), service60.getId(), MONDAY, TZ);

        assertThat(slots).doesNotContain(LocalTime.of(10, 0));  // exact overlap
        assertThat(slots).doesNotContain(LocalTime.of(10, 15)); // starts inside existing
        assertThat(slots).doesNotContain(LocalTime.of(9, 30));  // would end inside existing (9:30+60=10:30)
        assertThat(slots).contains(LocalTime.of(9, 0));         // ends at 10:00 — OK
        assertThat(slots).contains(LocalTime.of(11, 0));        // starts after existing ends
    }

    @Test @DisplayName("CANCELLED booking does not block slots")
    void cancelledBooking_doesNotBlock() {
        addRule(MONDAY.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.of(18, 0), true);

        ZoneId zone = ZoneId.of(TZ);
        Instant start = MONDAY.atTime(10, 0).atZone(zone).toInstant();
        Instant end   = MONDAY.atTime(11, 0).atZone(zone).toInstant();

        User client = userRepository.save(User.builder()
                .email("client2@slot.test").passwordHash(passwordEncoder.encode("p"))
                .fullName("Client2").role(UserRole.CLIENT).build());

        bookingRepository.save(Booking.builder()
                .client(client).service(service60).business(business).branch(branch)
                .employee(employee).startTime(start).endTime(end)
                .status(BookingStatus.CANCELLED)
                .durationMin(60).priceSnapshot(service60.getPrice())
                .selectedAttributes(new java.util.HashMap<>()).build());

        List<LocalTime> slots = slotGeneratorService
                .getAvailableSlotsForEmployee(employee.getId(), service60.getId(), MONDAY, TZ);

        // Cancelled booking should not block
        assertThat(slots).contains(LocalTime.of(10, 0));
    }

    // ── Helpers ───────────────────────────────────────────────────

    private ScheduleRule addRule(DayOfWeek day, LocalTime start, LocalTime end, boolean isWorking) {
        short dayOfWeek = (short) (day.getValue() % 7); // 0=Sun, 1=Mon, …
        return scheduleRuleRepository.save(ScheduleRule.builder()
                .employee(employee).branch(branch)
                .dayOfWeek(dayOfWeek)
                .startTime(start).endTime(end)
                .isWorkingDay(isWorking)
                .build());
    }
}
