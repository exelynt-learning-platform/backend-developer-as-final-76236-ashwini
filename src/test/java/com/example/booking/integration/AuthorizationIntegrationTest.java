package com.example.booking.integration;

import com.example.booking.dto.request.LoginRequest;
import com.example.booking.dto.request.ReservationRequest;
import com.example.booking.dto.request.ResourceRequest;
import com.example.booking.entity.User;
import com.example.booking.enums.Role;
import com.example.booking.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack authorization tests: exercises real HTTP requests through
 * Spring Security, JWT filter, controllers and H2 database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.example.booking.repository.ReservationRepository reservationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.save(User.builder()
                .name("Admin").email("admin@test.com")
                .password(passwordEncoder.encode("Admin@123")).role(Role.ADMIN).build());
        userRepository.save(User.builder()
                .name("User One").email("user1@test.com")
                .password(passwordEncoder.encode("User@123")).role(Role.USER).build());
        userRepository.save(User.builder()
                .name("User Two").email("user2@test.com")
                .password(passwordEncoder.encode("User@123")).role(Role.USER).build());
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest request = new LoginRequest(email, password);
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private ResourceRequest resourceRequest(String name, BigDecimal price) {
        ResourceRequest request = new ResourceRequest();
        request.setName(name);
        request.setDescription("desc");
        request.setPrice(price);
        request.setAvailable(true);
        return request;
    }

    @Test
    void login_withValidAdminCredentials_returns200AndToken() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin@test.com", "Admin@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_withInvalidPassword_returns401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin@test.com", "wrong"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/resources"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/resources").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void user_cannotCreateResource_returns403() throws Exception {
        String token = loginAndGetToken("user1@test.com", "User@123");

        mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resourceRequest("Test Room", new BigDecimal("10.00")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_canCreateResource_returns201() throws Exception {
        String token = loginAndGetToken("admin@test.com", "Admin@123");

        mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resourceRequest("Test Room", new BigDecimal("10.00")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Room"));
    }

    @Test
    void user_cannotAccessAnotherUsersReservation_returns403() throws Exception {
        String adminToken = loginAndGetToken("admin@test.com", "Admin@123");
        String resourceResponse = mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resourceRequest("Shared Room", new BigDecimal("25.00")))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long resourceId = objectMapper.readTree(resourceResponse).get("id").asLong();

        String user1Token = loginAndGetToken("user1@test.com", "User@123");
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setResourceId(resourceId);
        reservationRequest.setStartTime(LocalDateTime.now().plusDays(1));
        reservationRequest.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        reservationRequest.setPrice(new BigDecimal("25.00"));

        String reservationResponse = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reservationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userEmail").value("user1@test.com"))
                .andReturn().getResponse().getContentAsString();
        long reservationId = objectMapper.readTree(reservationResponse).get("id").asLong();

        // user2 must NOT be able to see user1's reservation
        String user2Token = loginAndGetToken("user2@test.com", "User@123");
        mockMvc.perform(get("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());

        // admin CAN see it
        mockMvc.perform(get("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void user_cannotSpoofOwnerViaRequestBody() throws Exception {
        String adminToken = loginAndGetToken("admin@test.com", "Admin@123");
        String resourceResponse = mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resourceRequest("Room X", new BigDecimal("15.00")))))
                .andReturn().getResponse().getContentAsString();
        long resourceId = objectMapper.readTree(resourceResponse).get("id").asLong();

        String user1Token = loginAndGetToken("user1@test.com", "User@123");

        // Even if a userId-like field were injected, ReservationRequest has no such
        // field at all, so the JSON below simply gets ignored by Jackson - proving
        // there's no way to influence ownership from the client.
        String maliciousJson = String.format("""
                {
                  "resourceId": %d,
                  "userId": 999,
                  "startTime": "%s",
                  "endTime": "%s",
                  "price": 15.00
                }
                """, resourceId, LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1));

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userEmail").value("user1@test.com"));
    }

    @Test
    void resources_pagination_returnsExpectedMetadata() throws Exception {
        String token = loginAndGetToken("user1@test.com", "User@123");
        mockMvc.perform(get("/resources?page=0&size=10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }
}