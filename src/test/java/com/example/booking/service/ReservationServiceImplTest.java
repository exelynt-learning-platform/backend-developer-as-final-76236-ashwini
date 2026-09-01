package com.example.booking.service;

import com.example.booking.dto.request.ReservationRequest;
import com.example.booking.dto.response.ReservationResponse;
import com.example.booking.entity.Reservation;
import com.example.booking.entity.Resource;
import com.example.booking.entity.User;
import com.example.booking.enums.ReservationStatus;
import com.example.booking.enums.Role;
import com.example.booking.exception.ConflictException;
import com.example.booking.exception.ForbiddenException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.repository.ReservationRepository;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.security.SecurityUtils;
import com.example.booking.service.impl.ReservationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private User owner;
    private User otherUser;
    private Resource resource;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).name("Owner").email("owner@example.com").password("x").role(Role.USER).build();
        otherUser = User.builder().id(2L).name("Other").email("other@example.com").password("x").role(Role.USER).build();
        resource = Resource.builder().id(10L).name("Room A").price(new BigDecimal("50")).available(true).build();
    }

    private ReservationRequest validRequest() {
        ReservationRequest request = new ReservationRequest();
        request.setResourceId(10L);
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        request.setPrice(new BigDecimal("100"));
        return request;
    }

    @Test
    void create_ownerAlwaysComesFromSecurityContext_neverFromRequest() {
        ReservationRequest request = validRequest();

        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));
        when(reservationRepository.findOverlapping(eq(10L), any(), any(), isNull())).thenReturn(List.of());
        when(securityUtils.getCurrentUser()).thenReturn(owner);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setId(100L);
            r.setCreatedAt(LocalDateTime.now());
            r.setUpdatedAt(LocalDateTime.now());
            return r;
        });

        ReservationResponse response = reservationService.create(request);

        assertThat(response.getUserId()).isEqualTo(owner.getId());
        assertThat(response.getUserId()).isNotEqualTo(otherUser.getId());

        // Verify the reservation persisted is bound to the authenticated user, not any request field
        verify(reservationRepository).save(argThat(r -> r.getUser().getId().equals(owner.getId())));
    }

    @Test
    void create_withEndTimeBeforeStartTime_throwsIllegalArgument() {
        ReservationRequest request = validRequest();
        request.setEndTime(request.getStartTime().minusHours(1));

        assertThatThrownBy(() -> reservationService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endTime must be after startTime");

        verifyNoInteractions(reservationRepository);
    }

    @Test
    void create_withUnavailableResource_throwsConflict() {
        resource.setAvailable(false);
        ReservationRequest request = validRequest();
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));

        assertThatThrownBy(() -> reservationService.create(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_withOverlappingReservation_throwsConflict() {
        ReservationRequest request = validRequest();
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));
        when(reservationRepository.findOverlapping(eq(10L), any(), any(), isNull()))
                .thenReturn(List.of(new Reservation()));

        assertThatThrownBy(() -> reservationService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("overlapping");
    }

    @Test
    void create_withUnknownResource_throwsNotFound() {
        ReservationRequest request = validRequest();
        when(resourceRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_whenUserAccessesAnotherUsersReservation_throwsForbidden() {
        Reservation reservation = Reservation.builder()
                .id(5L).resource(resource).user(owner)
                .startTime(LocalDateTime.now()).endTime(LocalDateTime.now().plusHours(1))
                .price(BigDecimal.TEN).status(ReservationStatus.PENDING)
                .build();

        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(otherUser);

        assertThatThrownBy(() -> reservationService.getById(5L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getById_whenAdmin_canAccessAnyReservation() {
        Reservation reservation = Reservation.builder()
                .id(5L).resource(resource).user(owner)
                .startTime(LocalDateTime.now()).endTime(LocalDateTime.now().plusHours(1))
                .price(BigDecimal.TEN).status(ReservationStatus.PENDING)
                .build();

        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));
        when(securityUtils.isAdmin()).thenReturn(true);

        ReservationResponse response = reservationService.getById(5L);

        assertThat(response.getId()).isEqualTo(5L);
        verify(securityUtils, never()).getCurrentUser();
    }

    @Test
    void getById_whenOwnerAccessesOwnReservation_succeeds() {
        Reservation reservation = Reservation.builder()
                .id(5L).resource(resource).user(owner)
                .startTime(LocalDateTime.now()).endTime(LocalDateTime.now().plusHours(1))
                .price(BigDecimal.TEN).status(ReservationStatus.PENDING)
                .build();

        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(owner);

        ReservationResponse response = reservationService.getById(5L);

        assertThat(response.getUserId()).isEqualTo(owner.getId());
    }
}
