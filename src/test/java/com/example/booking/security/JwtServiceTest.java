package com.example.booking.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("test-secret-key-for-jwt-tests-must-be-long-enough-1234567890", 3600000L);
        userDetails = org.springframework.security.core.userdetails.User
                .withUsername("user@example.com")
                .password("x")
                .authorities("ROLE_USER")
                .build();
    }

    @Test
    void generateToken_thenExtractUsername_matchesOriginal() {
        String token = jwtService.generateToken(userDetails, "USER");
        assertThat(jwtService.extractUsername(token)).isEqualTo("user@example.com");
    }

    @Test
    void generateToken_embedsRoleClaim() {
        String token = jwtService.generateToken(userDetails, "ADMIN");
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void isTokenValid_forMatchingUser_returnsTrue() {
        String token = jwtService.generateToken(userDetails, "USER");
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_forDifferentUser_returnsFalse() {
        String token = jwtService.generateToken(userDetails, "USER");
        UserDetails another = org.springframework.security.core.userdetails.User
                .withUsername("someone-else@example.com").password("x").authorities("ROLE_USER").build();
        assertThat(jwtService.isTokenValid(token, another)).isFalse();
    }
}
