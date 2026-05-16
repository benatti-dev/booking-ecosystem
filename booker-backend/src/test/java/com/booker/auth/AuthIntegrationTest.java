package com.booker.auth;

import com.booker.auth.dto.LoginRequest;
import com.booker.auth.dto.RegisterRequest;
import com.booker.auth.dto.TokenRequest;
import com.booker.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack auth integration tests.
 * Skipped automatically when Docker is not available.
 * Run with Docker Desktop active, or: mvn test -Dtest=AuthIntegrationTest
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@DisplayName("Auth Integration Tests")
class AuthIntegrationTest {

    @Autowired MockMvc        mockMvc;
    @Autowired ObjectMapper   objectMapper;
    @Autowired UserRepository userRepository;

    private static final String BASE     = "/auth";
    private static final String EMAIL    = "test@booker.app";
    private static final String PASSWORD = "Secure123!";

    @BeforeEach
    void cleanup() { userRepository.deleteAll(); }

    // Register

    @Test @DisplayName("POST /register - 201 + tokens in body")
    void register_success() throws Exception {
        mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(EMAIL, PASSWORD, "Test User", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
        assertThat(userRepository.existsByEmail(EMAIL)).isTrue();
    }

    @Test @DisplayName("POST /register - duplicate email -> 409")
    void register_duplicateEmail() throws Exception {
        doRegister();
        mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(EMAIL, PASSWORD, "Test User", null))))
                .andExpect(status().isConflict());
    }

    @Test @DisplayName("POST /register - invalid email -> 400")
    void register_invalidEmail() throws Exception {
        mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest("bad", PASSWORD, "Test", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
    }

    @Test @DisplayName("POST /register - short password -> 400")
    void register_shortPassword() throws Exception {
        mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(EMAIL, "short", "Test", null))))
                .andExpect(status().isBadRequest());
    }

    // Login

    @Test @DisplayName("POST /login - 200 + tokens")
    void login_success() throws Exception {
        doRegister();
        mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(EMAIL, PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test @DisplayName("POST /login - wrong password -> 401")
    void login_wrongPassword() throws Exception {
        doRegister();
        mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(EMAIL, "wrongpassword"))))
                .andExpect(status().isUnauthorized());
    }

    // /me

    @Test @DisplayName("GET /me - valid Bearer -> 200")
    void me_withValidToken() throws Exception {
        String token = tokens().accessToken();
        mockMvc.perform(get(BASE + "/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    @Test @DisplayName("GET /me - no token -> 403")
    void me_withoutToken() throws Exception {
        mockMvc.perform(get(BASE + "/me")).andExpect(status().isForbidden());
    }

    // Refresh

    @Test @DisplayName("POST /refresh - valid body -> new tokens")
    void refresh_success() throws Exception {
        TokenPair t = tokens();
        mockMvc.perform(post(BASE + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new TokenRequest(t.refreshToken()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test @DisplayName("POST /refresh - invalid token -> 401")
    void refresh_invalidToken() throws Exception {
        mockMvc.perform(post(BASE + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new TokenRequest("invalid"))))
                .andExpect(status().isUnauthorized());
    }

    // Logout

    @Test @DisplayName("POST /logout - revokes token; refresh -> 401")
    void logout_revokesToken() throws Exception {
        TokenPair t = tokens();
        mockMvc.perform(post(BASE + "/logout")
                        .header("Authorization", "Bearer " + t.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new TokenRequest(t.refreshToken()))))
                .andExpect(status().isOk());
        mockMvc.perform(post(BASE + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new TokenRequest(t.refreshToken()))))
                .andExpect(status().isUnauthorized());
    }

    // Helpers

    private record TokenPair(String accessToken, String refreshToken) {}

    private void doRegister() throws Exception {
        mockMvc.perform(post(BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new RegisterRequest(EMAIL, PASSWORD, "Test User", null)))).andReturn();
    }

    private TokenPair tokens() throws Exception {
        MvcResult r = mockMvc.perform(post(BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new RegisterRequest(EMAIL, PASSWORD, "Test User", null)))).andReturn();
        JsonNode body = objectMapper.readTree(r.getResponse().getContentAsString());
        return new TokenPair(body.get("accessToken").asText(), body.get("refreshToken").asText());
    }

    private String json(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}