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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Concurrency / race-condition integration test for the booking advisory-lock mechanism.
 *
 * Verifies that when N clients simultaneously attempt to book the same employee slot,
 * exactly ONE succeeds (HTTP 201) and all others are rejected (HTTP 409).
 *
 * Uses a real PostgreSQL container via Testcontainers and the full Spring Boot context
 * so that the advisory lock path in {@link com.booker.booking.service.BookingService} is exercised.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@DisplayName("Booking Concurrency — Race Condition Integration Tests")
class BookingConcurrencyIntegrationTest {

    @Autowired MockMvc        mockMvc;
    @Autowired ObjectMapper   objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired BusinessCategoryRepository categoryRepository;
    @Autowired BusinessRepository         businessRepository;
    @Autowired BranchRepository           branchRepository;
    @Autowired ServiceRepository          serviceRepository;
    @Autowired EmployeeRepository         employeeRepository;
    @Autowired ScheduleRuleRepository     scheduleRuleRepository;
    @Autowired BookingRepository          bookingRepository;
    @Autowired BookingCancellationRepository cancellationRepository;
    @Autowired PasswordEncoder            passwordEncoder;

    private static final String PASSWORD = "Secure123!";

    private long   serviceId;
    private long   employeeId;
    private long   branchId;
    private Instant slotStart;

    /** Tokens for 5 concurrent clients. */
    private final List<String> clientTokens = new ArrayList<>();

    @BeforeEach
    void setup() throws Exception {
        cancellationRepository.deleteAll();
        bookingRepository.deleteAll();
        scheduleRuleRepository.deleteAll();
        employeeRepository.deleteAll();
        serviceRepository.deleteAll();
        branchRepository.deleteAll();
        businessRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        clientTokens.clear();

        User owner = createUser("owner@race.test", UserRole.BUSINESS_OWNER);

        BusinessCategory cat = categoryRepository.save(BusinessCategory.builder()
                .name("beauty").label("Beauty").resourceType(ResourceType.EMPLOYEE).build());

        Business business = businessRepository.save(Business.builder()
                .owner(owner).category(cat).name("Race Salon")
                .status(BusinessStatus.ACTIVE).build());

        branchId = branchRepository.save(Branch.builder()
                .business(business).name("Branch")
                .address("1 St").city("Kyiv")
                .timezone("Europe/Kyiv").build()).getId();

        serviceId = serviceRepository.save(Service.builder()
                .business(business).category(cat)
                .name("Cut").durationMin(60)
                .price(BigDecimal.valueOf(300)).isActive(true).build()).getId();

        employeeId = employeeRepository.save(Employee.builder()
                .business(business).branch(branchRepository.findById(branchId).orElseThrow())
                .displayName("Stylist").isActive(true).build()).getId();

        // Working every day Mon–Sun 09:00–18:00
        for (int day = 1; day <= 7; day++) {
            scheduleRuleRepository.save(ScheduleRule.builder()
                    .employee(employeeRepository.findById(employeeId).orElseThrow())
                    .branch(branchRepository.findById(branchId).orElseThrow())
                    .dayOfWeek((short) day)
                    .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(18, 0))
                    .isWorkingDay(true).build());
        }

        // Fixed slot two weeks from now at 10:00 UTC so it never falls outside working hours
        slotStart = Instant.now().truncatedTo(ChronoUnit.DAYS)
                .plus(14, ChronoUnit.DAYS)
                .plus(10, ChronoUnit.HOURS);

        // Create 5 clients, collect their tokens
        for (int i = 1; i <= 5; i++) {
            String email = "client" + i + "@race.test";
            createUser(email, UserRole.CLIENT);
            clientTokens.add(login(email));
        }
    }

    // ── Race Condition Test ────────────────────────────────────────────────

    @Test
    @DisplayName("5 concurrent requests for same slot → exactly 1 succeeds, 4 rejected (409)")
    void concurrentBooking_sameSlot_onlyOneSucceeds() throws Exception {
        int threadCount = 5;
        ExecutorService pool   = Executors.newFixedThreadPool(threadCount);
        CountDownLatch  ready  = new CountDownLatch(threadCount);
        CountDownLatch  go     = new CountDownLatch(1);

        AtomicInteger created   = new AtomicInteger(0);
        AtomicInteger conflicts = new AtomicInteger(0);
        List<Future<Integer>> futures = new ArrayList<>();

        String body = json(Map.of(
                "serviceId",  serviceId,
                "employeeId", employeeId,
                "branchId",   branchId,
                "startTime",  slotStart.toString()
        ));

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                ready.countDown();   // signal this thread is ready
                go.await();          // wait for all threads to be ready before firing
                MvcResult result = mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + clientTokens.get(idx))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)).andReturn();
                return result.getResponse().getStatus();
            }));
        }

        ready.await();  // wait until all threads are ready
        go.countDown(); // release all threads simultaneously

        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);

        for (Future<Integer> f : futures) {
            int status = f.get();
            if (status == 201) created.incrementAndGet();
            else if (status == 409) conflicts.incrementAndGet();
        }

        assertThat(created.get())
                .as("Exactly one booking should be created for the contested slot")
                .isEqualTo(1);
        assertThat(conflicts.get())
                .as("All other concurrent requests should receive 409 CONFLICT")
                .isEqualTo(threadCount - 1);
        assertThat(bookingRepository.count())
                .as("Database should contain exactly one booking")
                .isEqualTo(1);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

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
                .content(json(Map.of("username", email, "password", PASSWORD))))
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }
}
