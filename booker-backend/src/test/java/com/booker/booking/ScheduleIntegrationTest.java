package com.booker.booking;

import com.booker.auth.dto.LoginRequest;
import com.booker.auth.entity.User;
import com.booker.auth.entity.UserRole;
import com.booker.auth.repository.UserRepository;
import com.booker.booking.entity.BookingStatus;
import com.booker.booking.repository.*;
import com.booker.business.entity.*;
import com.booker.business.repository.*;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@DisplayName("Schedule Integration Tests")
class ScheduleIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired BusinessCategoryRepository categoryRepository;
    @Autowired BusinessRepository businessRepository;
    @Autowired BranchRepository branchRepository;
    @Autowired ServiceRepository serviceRepository;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired ScheduleRuleRepository scheduleRuleRepository;
    @Autowired ScheduleOverrideRepository scheduleOverrideRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired BookingCancellationRepository cancellationRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private static final String OWNER_EMAIL = "owner@schedule.test";
    private static final String PASSWORD    = "Secure123!";

    private Employee employee;

    @BeforeEach
    void setup() {
        cancellationRepository.deleteAll();
        bookingRepository.deleteAll();
        scheduleOverrideRepository.deleteAll();
        scheduleRuleRepository.deleteAll();
        employeeRepository.deleteAll();
        serviceRepository.deleteAll();
        branchRepository.deleteAll();
        businessRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        User owner = createUser(OWNER_EMAIL, UserRole.BUSINESS_OWNER);

        BusinessCategory cat = categoryRepository.save(BusinessCategory.builder()
                .name("beauty").label("Beauty").resourceType(ResourceType.EMPLOYEE).build());

        Business business = businessRepository.save(Business.builder()
                .owner(owner).category(cat).name("Schedule Salon")
                .status(BusinessStatus.ACTIVE).build());

        Branch branch = branchRepository.save(Branch.builder()
                .business(business).name("Branch").address("1 St")
                .city("Kyiv").timezone("Europe/Kiev").build());

        employee = employeeRepository.save(Employee.builder()
                .business(business).branch(branch)
                .displayName("Anna").isActive(true).build());
    }

    // ── GET /employees/{id}/schedule ──────────────────────────────

    @Test @DisplayName("GET schedule - no rules → empty rules list")
    void getSchedule_empty() throws Exception {
        String token = login(OWNER_EMAIL);

        mockMvc.perform(get("/employees/" + employee.getId() + "/schedule")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(employee.getId()))
                .andExpect(jsonPath("$.rules").isArray())
                .andExpect(jsonPath("$.rules", hasSize(0)));
    }

    @Test @DisplayName("GET schedule - unauthenticated → 403")
    void getSchedule_unauthenticated() throws Exception {
        mockMvc.perform(get("/employees/" + employee.getId() + "/schedule"))
                .andExpect(status().isForbidden());
    }

    // ── PUT /employees/{id}/schedule ──────────────────────────────

    @Test @DisplayName("PUT schedule - saves 7 rules and returns them")
    void saveSchedule_fullWeek() throws Exception {
        String token = login(OWNER_EMAIL);
        String body = weeklyScheduleJson();

        mockMvc.perform(put("/employees/" + employee.getId() + "/schedule")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rules", hasSize(7)));
    }

    @Test @DisplayName("PUT schedule - replaces existing rules on second call")
    void saveSchedule_replacesExisting() throws Exception {
        String token = login(OWNER_EMAIL);

        // First PUT: Mon–Fri working, Sat–Sun off
        mockMvc.perform(put("/employees/" + employee.getId() + "/schedule")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(weeklyScheduleJson())).andReturn();

        // Second PUT: only Monday working
        String singleDayBody = json(List.of(Map.of(
                "dayOfWeek", 1,
                "startTime", "09:00:00",
                "endTime", "17:00:00",
                "isWorkingDay", true,
                "breaks", List.of()
        )));

        mockMvc.perform(put("/employees/" + employee.getId() + "/schedule")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleDayBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rules", hasSize(1)));
    }

    @Test @DisplayName("PUT schedule - includes breaks in response")
    void saveSchedule_withBreaks() throws Exception {
        String token = login(OWNER_EMAIL);
        String body = json(List.of(Map.of(
                "dayOfWeek", 1,
                "startTime", "09:00:00",
                "endTime", "18:00:00",
                "isWorkingDay", true,
                "breaks", List.of(Map.of("startTime", "12:00:00", "endTime", "13:00:00"))
        )));

        mockMvc.perform(put("/employees/" + employee.getId() + "/schedule")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rules[0].breaks", hasSize(1)))
                .andExpect(jsonPath("$.rules[0].breaks[0].startTime").value("12:00:00"));
    }

    // ── POST /employees/{id}/schedule/overrides ───────────────────

    @Test @DisplayName("POST override - adds override for a specific date")
    void addOverride_success() throws Exception {
        String token = login(OWNER_EMAIL);
        LocalDate futureDate = LocalDate.now().plusDays(10);

        String body = json(Map.of(
                "overrideDate", futureDate.toString(),
                "reason", "National holiday"
        ));

        mockMvc.perform(post("/employees/" + employee.getId() + "/schedule/overrides")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overrideDate").value(futureDate.toString()))
                .andExpect(jsonPath("$.reason").value("National holiday"));
    }

    @Test @DisplayName("POST override - with custom hours")
    void addOverride_withCustomHours() throws Exception {
        String token = login(OWNER_EMAIL);

        String body = json(Map.of(
                "overrideDate", LocalDate.now().plusDays(5).toString(),
                "startTime", "14:00:00",
                "endTime", "17:00:00",
                "reason", "Shortened day"
        ));

        mockMvc.perform(post("/employees/" + employee.getId() + "/schedule/overrides")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime").value("14:00:00"))
                .andExpect(jsonPath("$.endTime").value("17:00:00"));
    }

    // ── Helpers ───────────────────────────────────────────────────

    private User createUser(String email, UserRole role) {
        return userRepository.save(User.builder()
                .email(email).passwordHash(passwordEncoder.encode(PASSWORD))
                .fullName("Test").role(role).build());
    }

    private String login(String email) throws Exception {
        MvcResult r = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", PASSWORD)))).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    /** Full 7-day weekly schedule: Mon–Fri working, Sat–Sun off. */
    private String weeklyScheduleJson() throws Exception {
        List<Map<String, Object>> rules = new java.util.ArrayList<>();
        for (int day = 0; day <= 6; day++) {
            boolean working = day >= 1 && day <= 5;
            rules.add(Map.of(
                    "dayOfWeek", day,
                    "startTime", "09:00:00",
                    "endTime", "18:00:00",
                    "isWorkingDay", working,
                    "breaks", List.of()
            ));
        }
        return json(rules);
    }

    private String json(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
