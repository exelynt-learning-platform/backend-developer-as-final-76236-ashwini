package com.example.booking.integration;

import com.example.booking.dto.request.LoginRequest;
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

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.example.booking.repository.ReservationRepository reservationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        reservationRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.save(User.builder()
                .name("Admin").email("admin@test.com")
                .password(passwordEncoder.encode("Admin@123")).role(Role.ADMIN).build());

        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin@test.com", "Admin@123"))))
                .andReturn().getResponse().getContentAsString();
        adminToken = objectMapper.readTree(response).get("token").asText();
    }

    @Test
    void createResource_withNegativePrice_returns400() throws Exception {
        String body = """
                { "name": "Room", "description": "d", "price": -5.00, "available": true }
                """;
        mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createResource_withMissingName_returns400() throws Exception {
        String body = """
                { "description": "d", "price": 5.00, "available": true }
                """;
        mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReservation_withEndTimeBeforeStartTime_returns400() throws Exception {
        // First create a resource to reference
        String resourceResponse = mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Room", "description": "d", "price": 10.00, "available": true }
                                """))
                .andReturn().getResponse().getContentAsString();
        long resourceId = objectMapper.readTree(resourceResponse).get("id").asLong();

        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.minusHours(2);
        String body = String.format("""
                { "resourceId": %d, "startTime": "%s", "endTime": "%s", "price": 10.00 }
                """, resourceId, start, end);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReservation_withMissingResourceId_returns400() throws Exception {
        String body = """
                { "startTime": "2026-09-10T10:00:00", "endTime": "2026-09-10T12:00:00", "price": 10.00 }
                """;
        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}