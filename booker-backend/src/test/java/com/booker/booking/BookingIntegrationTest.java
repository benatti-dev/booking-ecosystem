package com.booker.booking;

import com.booker.auth.dto.LoginRequest;
import com.booker.auth.entity.User;
import com.booker.auth.entity.UserRole;
import com.booker.auth.repository.UserRepository;
import com.booker.booking.entity.*;
import com.booker.booking.repository.*;
import com.booker.business.entity.*;
import com.booker.business.repository.*;
import com.booker.catalog.entity.Service;
import com.booker.catalog.repository.ServiceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@DisplayName("Booking Integration Tests")
class BookingIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired BusinessCategoryRepository categoryRepository;
    @Autowired BusinessRepository businessRepository;
    @Autowired BranchRepository branchRepository;
    @Autowired ServiceRepository serviceRepository;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired ScheduleRuleRepository scheduleRuleRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired BookingCancellationRepository cancellationRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private static final String CLIENT_EMAIL = "client@booker.app";
    private static final String OWNER_EMAIL  = "owner@booker.app";
    private static final String ADMIN_EMAIL  = "admin@booker.app";
    private static final String PASSWORD     = "Secure123!";

    // Shared entities
    private Branch branch;
    private Service service;
    private Employee employee;

    @BeforeEach
    void setup() {
        cancellationRepository.deleteAll();
        bookingRepository.deleteAll();
        scheduleRuleRepository.deleteAll();
        employeeRepository.deleteAll();
        serviceRepository.deleteAll();
        branchRepository.deleteAll();
        businessRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        // Users
        User owner  = createUser(OWNER_EMAIL, UserRole.BUSINESS_OWNER);
        User client = createUser(CLIENT_EMAIL, UserRole.CLIENT);
        createUser(ADMIN_EMAIL, UserRole.ADMIN);

        // Business setup
        BusinessCategory cat = categoryRepository.save(BusinessCategory.builder()
                .name("beauty").label("Beauty").resourceType(ResourceType.EMPLOYEE).build());

        Business business = businessRepository.save(Business.builder()
                .owner(owner).category(cat).name("Test Salon")
                .status(BusinessStatus.ACTIVE).build());

        branch = branchRepository.save(Branch.builder()
                .business(business).name("Main Branch")
                .address("123 Main St").city("Kyiv")
                .timezone("Europe/Kiev").build());

        service = serviceRepository.save(Service.builder()
                .business(business).category(cat)
                .name("Haircut").durationMin(60)
                .price(BigDecimal.valueOf(500)).isActive(true).build());

        employee = employeeRepository.save(Employee.builder()
                .business(business).branch(branch)
                .displayName("Jane Doe").isActive(true).build());

        // Working schedule: Mon–Fri 09:00–18:00
        for (int day = 1; day <= 5; day++) {
            scheduleRuleRepository.save(ScheduleRule.builder()
                    .employee(employee).branch(branch).dayOfWeek((short) day)
                    .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(18, 0))
                    .isWorkingDay(true).build());
        }
    }

    // ── POST /bookings ────────────────────────────────────────────

    @Test @DisplayName("POST /bookings - client creates booking → 201")
    void createBooking_success() throws Exception {
        String token = login(CLIENT_EMAIL);
        Instant start = nextWeekday().plus(9, ChronoUnit.HOURS);

        mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(start)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.serviceName").value("Haircut"))
                .andExpect(jsonPath("$.employeeName").value("Jane Doe"));
    }

    @Test @DisplayName("POST /bookings - non-CLIENT role → 403")
    void createBooking_nonClientForbidden() throws Exception {
        String ownerToken = login(OWNER_EMAIL);
        Instant start = nextWeekday().plus(9, ChronoUnit.HOURS);

        mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(start)))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("POST /bookings - missing service → 404")
    void createBooking_unknownService() throws Exception {
        String token = login(CLIENT_EMAIL);

        mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "serviceId", 99999,
                                "employeeId", employee.getId(),
                                "branchId", branch.getId(),
                                "startTime", nextWeekday().plus(9, ChronoUnit.HOURS).toString()
                        ))))
                .andExpect(status().isNotFound());
    }

    // ── PATCH /bookings/{id}/confirm ─────────────────────────────

    @Test @DisplayName("PATCH /confirm - PENDING → CONFIRMED (owner) → 200")
    void confirmBooking_success() throws Exception {
        long id = createBooking();
        String ownerToken = login(OWNER_EMAIL);

        mockMvc.perform(patch("/bookings/" + id + "/confirm")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test @DisplayName("PATCH /confirm - already CONFIRMED → 400")
    void confirmBooking_alreadyConfirmed() throws Exception {
        long id = createBooking();
        String ownerToken = login(OWNER_EMAIL);

        // First confirm
        mockMvc.perform(patch("/bookings/" + id + "/confirm")
                .header("Authorization", "Bearer " + ownerToken)).andReturn();

        // Second confirm attempt
        mockMvc.perform(patch("/bookings/" + id + "/confirm")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest());
    }

    @Test @DisplayName("PATCH /confirm - by CLIENT → 403")
    void confirmBooking_clientForbidden() throws Exception {
        long id = createBooking();
        String clientToken = login(CLIENT_EMAIL);

        mockMvc.perform(patch("/bookings/" + id + "/confirm")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    // ── PATCH /bookings/{id}/complete ─────────────────────────────

    @Test @DisplayName("PATCH /complete - CONFIRMED → COMPLETED (owner) → 200")
    void completeBooking_success() throws Exception {
        long id = createBooking();
        String ownerToken = login(OWNER_EMAIL);

        // Confirm first
        mockMvc.perform(patch("/bookings/" + id + "/confirm")
                .header("Authorization", "Bearer " + ownerToken)).andReturn();

        mockMvc.perform(patch("/bookings/" + id + "/complete")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test @DisplayName("PATCH /complete - PENDING (not confirmed) → 400")
    void completeBooking_notConfirmed() throws Exception {
        long id = createBooking();
        String ownerToken = login(OWNER_EMAIL);

        mockMvc.perform(patch("/bookings/" + id + "/complete")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest());
    }

    // ── PATCH /bookings/{id}/cancel ──────────────────────────────

    @Test @DisplayName("PATCH /cancel - owner can cancel any booking → 200")
    void cancelBooking_owner() throws Exception {
        long id = createBooking();
        String ownerToken = login(OWNER_EMAIL);

        mockMvc.perform(patch("/bookings/" + id + "/cancel")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("reason", "Client no-show"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test @DisplayName("PATCH /cancel - client cancels own booking with > 24h notice → 200")
    void cancelBooking_clientWithEnoughNotice() throws Exception {
        long id = createBooking(); // start is ~2 days from now
        String clientToken = login(CLIENT_EMAIL);

        mockMvc.perform(patch("/bookings/" + id + "/cancel")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("reason", "Changed my mind"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test @DisplayName("PATCH /cancel - client cancels with < 24h notice → 400")
    void cancelBooking_tooLate() throws Exception {
        // Create a booking with start time in 2 hours
        String clientToken = login(CLIENT_EMAIL);
        Instant twoHoursFromNow = Instant.now().plus(2, ChronoUnit.HOURS);

        MvcResult r = mockMvc.perform(post("/bookings")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "serviceId", service.getId(),
                        "employeeId", employee.getId(),
                        "branchId", branch.getId(),
                        "startTime", twoHoursFromNow.toString()
                )))).andReturn();

        long id = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/bookings/" + id + "/cancel")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("reason", "Too late"))))
                .andExpect(status().isBadRequest());
    }

    @Test @DisplayName("PATCH /cancel - cancel COMPLETED booking → 400")
    void cancelBooking_alreadyCompleted() throws Exception {
        long id = createBooking();
        String ownerToken = login(OWNER_EMAIL);

        // Confirm then complete
        mockMvc.perform(patch("/bookings/" + id + "/confirm")
                .header("Authorization", "Bearer " + ownerToken)).andReturn();
        mockMvc.perform(patch("/bookings/" + id + "/complete")
                .header("Authorization", "Bearer " + ownerToken)).andReturn();

        mockMvc.perform(patch("/bookings/" + id + "/cancel")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("reason", "Oops"))))
                .andExpect(status().isBadRequest());
    }

    @Test @DisplayName("PATCH /cancel - client cannot cancel another client's booking → 403")
    void cancelBooking_wrongClient() throws Exception {
        // Create second client
        createUser("other@booker.app", UserRole.CLIENT);
        String otherToken = login("other@booker.app");

        long id = createBooking();

        mockMvc.perform(patch("/bookings/" + id + "/cancel")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("reason", "Not mine"))))
                .andExpect(status().isForbidden());
    }

    // ── GET /bookings/my ─────────────────────────────────────────

    @Test @DisplayName("GET /bookings/my - returns client's bookings")
    void getMyBookings() throws Exception {
        createBooking();
        createBooking();
        String clientToken = login(CLIENT_EMAIL);

        mockMvc.perform(get("/bookings/my")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    // ── GET /bookings/{id} ────────────────────────────────────────

    @Test @DisplayName("GET /bookings/{id} - owner accesses → 200")
    void getBookingById_owner() throws Exception {
        long id = createBooking();
        String ownerToken = login(OWNER_EMAIL);

        mockMvc.perform(get("/bookings/" + id)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test @DisplayName("GET /bookings/{id} - not found → 404")
    void getBookingById_notFound() throws Exception {
        String token = login(OWNER_EMAIL);

        mockMvc.perform(get("/bookings/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ── Helpers ───────────────────────────────────────────────────

    /** Returns the start of the next Monday at midnight UTC, shifted to next weekday. */
    private Instant nextWeekday() {
        return Instant.now().truncatedTo(ChronoUnit.DAYS).plus(2, ChronoUnit.DAYS);
    }

    private String bookingJson(Instant start) throws Exception {
        return json(Map.of(
                "serviceId", service.getId(),
                "employeeId", employee.getId(),
                "branchId", branch.getId(),
                "startTime", start.toString()
        ));
    }

    /** Creates a booking as CLIENT with start 2 days from now and returns id. */
    private long createBooking() throws Exception {
        String clientToken = login(CLIENT_EMAIL);
        MvcResult r = mockMvc.perform(post("/bookings")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingJson(nextWeekday().plus(9, ChronoUnit.HOURS))))
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private User createUser(String email, UserRole role) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .fullName("Test " + role)
                .role(role)
                .build());
    }

    private String login(String email) throws Exception {
        MvcResult r = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new LoginRequest(email, PASSWORD)))).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private String json(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
