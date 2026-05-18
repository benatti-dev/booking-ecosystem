package com.booker.business;

import com.booker.auth.dto.LoginRequest;
import com.booker.auth.dto.RegisterRequest;
import com.booker.auth.entity.User;
import com.booker.auth.entity.UserRole;
import com.booker.auth.repository.UserRepository;
import com.booker.business.entity.BusinessCategory;
import com.booker.business.entity.ResourceType;
import com.booker.business.repository.BusinessCategoryRepository;
import com.booker.business.repository.BusinessRepository;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@DisplayName("Business Integration Tests")
class BusinessIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired BusinessRepository businessRepository;
    @Autowired BusinessCategoryRepository categoryRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private static final String OWNER_EMAIL  = "owner@booker.app";
    private static final String OTHER_EMAIL  = "other@booker.app";
    private static final String ADMIN_EMAIL  = "admin@booker.app";
    private static final String PASSWORD     = "Secure123!";

    private BusinessCategory category;

    @BeforeEach
    void setup() {
        businessRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        category = categoryRepository.save(BusinessCategory.builder()
                .name("beauty")
                .label("Beauty Salon")
                .resourceType(ResourceType.EMPLOYEE)
                .build());

        createUser(OWNER_EMAIL, UserRole.BUSINESS_OWNER);
        createUser(OTHER_EMAIL, UserRole.BUSINESS_OWNER);
        createUser(ADMIN_EMAIL, UserRole.ADMIN);
    }

    // ── POST /businesses ──────────────────────────────────────────

    @Test @DisplayName("POST /businesses - owner creates business → 201")
    void createBusiness_success() throws Exception {
        String token = login(OWNER_EMAIL);

        mockMvc.perform(post("/businesses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "categoryId", category.getId(),
                                "name", "Glam Studio",
                                "description", "Top beauty salon"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Glam Studio"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.category.name").value("beauty"));
    }

    @Test @DisplayName("POST /businesses - unknown category → 404")
    void createBusiness_unknownCategory() throws Exception {
        String token = login(OWNER_EMAIL);

        mockMvc.perform(post("/businesses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "categoryId", 99999,
                                "name", "Ghost Business"
                        ))))
                .andExpect(status().isNotFound());
    }

    @Test @DisplayName("POST /businesses - unauthenticated → 403")
    void createBusiness_noToken() throws Exception {
        mockMvc.perform(post("/businesses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("categoryId", category.getId(), "name", "X"))))
                .andExpect(status().isForbidden());
    }

    // ── GET /businesses/{id} ──────────────────────────────────────

    @Test @DisplayName("GET /businesses/{id} - existing business → 200")
    void getBusiness_success() throws Exception {
        long id = createBusiness(OWNER_EMAIL);

        mockMvc.perform(get("/businesses/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Test Business"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test @DisplayName("GET /businesses/{id} - not found → 404")
    void getBusiness_notFound() throws Exception {
        mockMvc.perform(get("/businesses/999999"))
                .andExpect(status().isNotFound());
    }

    // ── PUT /businesses/{id} ──────────────────────────────────────

    @Test @DisplayName("PUT /businesses/{id} - owner updates → 200")
    void updateBusiness_owner() throws Exception {
        long id = createBusiness(OWNER_EMAIL);
        String token = login(OWNER_EMAIL);

        mockMvc.perform(put("/businesses/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Updated Name"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test @DisplayName("PUT /businesses/{id} - different owner → 403")
    void updateBusiness_forbidden() throws Exception {
        long id = createBusiness(OWNER_EMAIL);
        String otherToken = login(OTHER_EMAIL);

        mockMvc.perform(put("/businesses/" + id)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Hijack"))))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("PUT /businesses/{id} - admin can update any → 200")
    void updateBusiness_admin() throws Exception {
        long id = createBusiness(OWNER_EMAIL);
        String adminToken = login(ADMIN_EMAIL);

        mockMvc.perform(put("/businesses/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("description", "Admin updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Admin updated"));
    }

    // ── PATCH /businesses/{id}/status ─────────────────────────────

    @Test @DisplayName("PATCH status PENDING → ACTIVE (admin) → 200")
    void changeStatus_pendingToActive() throws Exception {
        long id = createBusiness(OWNER_EMAIL);
        String adminToken = login(ADMIN_EMAIL);

        mockMvc.perform(patch("/businesses/" + id + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "ACTIVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test @DisplayName("PATCH status ACTIVE → SUSPENDED (admin) → 200")
    void changeStatus_activeToSuspended() throws Exception {
        long id = activateBusiness(OWNER_EMAIL);
        String adminToken = login(ADMIN_EMAIL);

        mockMvc.perform(patch("/businesses/" + id + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "SUSPENDED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test @DisplayName("PATCH status PENDING → SUSPENDED (invalid transition) → 400")
    void changeStatus_invalidTransition() throws Exception {
        long id = createBusiness(OWNER_EMAIL);
        String adminToken = login(ADMIN_EMAIL);

        mockMvc.perform(patch("/businesses/" + id + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "SUSPENDED"))))
                .andExpect(status().isBadRequest());
    }

    @Test @DisplayName("PATCH status - non-admin → 403")
    void changeStatus_notAdmin() throws Exception {
        long id = createBusiness(OWNER_EMAIL);
        String ownerToken = login(OWNER_EMAIL);

        mockMvc.perform(patch("/businesses/" + id + "/status")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "ACTIVE"))))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("PATCH status PENDING → REJECTED (admin) → 200")
    void changeStatus_pendingToRejected() throws Exception {
        long id = createBusiness(OWNER_EMAIL);
        String adminToken = login(ADMIN_EMAIL);

        mockMvc.perform(patch("/businesses/" + id + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "REJECTED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    // ── GET /businesses/my ────────────────────────────────────────

    @Test @DisplayName("GET /businesses/my - returns only owner's businesses")
    void getMyBusinesses() throws Exception {
        createBusiness(OWNER_EMAIL);
        createBusiness(OWNER_EMAIL);
        createBusiness(OTHER_EMAIL);
        String token = login(OWNER_EMAIL);

        mockMvc.perform(get("/businesses/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    // ── Helpers ───────────────────────────────────────────────────

    private void createUser(String email, UserRole role) {
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .fullName("Test User")
                .role(role)
                .build();
        userRepository.save(user);
    }

    private String login(String email) throws Exception {
        MvcResult r = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new LoginRequest(email, PASSWORD)))).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    /** Creates a business (PENDING) and returns its id. */
    private long createBusiness(String ownerEmail) throws Exception {
        String token = login(ownerEmail);
        MvcResult r = mockMvc.perform(post("/businesses")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "categoryId", category.getId(),
                        "name", "Test Business"
                )))).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    /** Creates a business and activates it, returns its id. */
    private long activateBusiness(String ownerEmail) throws Exception {
        long id = createBusiness(ownerEmail);
        String adminToken = login(ADMIN_EMAIL);
        mockMvc.perform(patch("/businesses/" + id + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("status", "ACTIVE")))).andReturn();
        return id;
    }

    private String json(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
