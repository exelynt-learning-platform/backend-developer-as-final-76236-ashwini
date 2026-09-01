package com.example.booking.service;

import com.example.booking.dto.request.LoginRequest;
import com.example.booking.dto.response.LoginResponse;
import com.example.booking.entity.User;
import com.example.booking.enums.Role;
import com.example.booking.exception.UnauthorizedException;
import com.example.booking.repository.UserRepository;
import com.example.booking.security.JwtService;
import com.example.booking.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .name("Admin")
                .email("admin@example.com")
                .password("encoded-password")
                .role(Role.ADMIN)
                .build();
    }

    @Test
    void login_withValidCredentials_returnsTokenAndRole() {
        LoginRequest request = new LoginRequest("admin@example.com", "Admin@123");
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("admin@example.com").password("x").authorities("ROLE_ADMIN").build();

        when(userDetailsService.loadUserByUsername("admin@example.com")).thenReturn(userDetails);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(jwtService.generateToken(userDetails, "ADMIN")).thenReturn("mocked-jwt-token");

        LoginResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("mocked-jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getEmail()).isEqualTo("admin@example.com");
        assertThat(response.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void login_withInvalidPassword_throwsUnauthorized() {
        LoginRequest request = new LoginRequest("admin@example.com", "wrong-password");
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_withUnknownEmail_throwsUnauthorized() {
        LoginRequest request = new LoginRequest("unknown@example.com", "whatever");
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }
}
