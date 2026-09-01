package com.example.booking.service.impl;

import com.example.booking.dto.request.ReservationRequest;
import com.example.booking.dto.response.PagedResponse;
import com.example.booking.dto.response.ReservationResponse;
import com.example.booking.entity.Reservation;
import com.example.booking.entity.Resource;
import com.example.booking.entity.User;
import com.example.booking.enums.ReservationStatus;
import com.example.booking.exception.ConflictException;
import com.example.booking.exception.ForbiddenException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.repository.ReservationRepository;
import com.example.booking.repository.ReservationSpecifications;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.security.SecurityUtils;
import com.example.booking.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public ReservationResponse create(ReservationRequest request) {
        validateTimeRange(request);

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + request.getResourceId()));

        if (!resource.isAvailable()) {
            throw new ConflictException("Resource is not available for booking");
        }

        assertNoOverlap(resource.getId(), request.getStartTime(), request.getEndTime(), null);

        // SECURITY: the owner is ALWAYS the currently authenticated user,
        // never a value supplied in the request body.
        User currentUser = securityUtils.getCurrentUser();

        Reservation reservation = Reservation.builder()
                .resource(resource)
                .user(currentUser)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .price(request.getPrice())
                .status(request.getStatus() != null ? request.getStatus() : ReservationStatus.PENDING)
                .build();

        return toResponse(reservationRepository.save(reservation));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReservationResponse> getAll(ReservationStatus status, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        User currentUser = securityUtils.getCurrentUser();

        Specification<Reservation> spec = Specification.where(ReservationSpecifications.hasStatus(status))
                .and(ReservationSpecifications.minPrice(minPrice))
                .and(ReservationSpecifications.maxPrice(maxPrice));

        // USER role is restricted to their own reservations; ADMIN sees everything.
        if (!securityUtils.isAdmin()) {
            spec = spec.and(ReservationSpecifications.belongsToUser(currentUser.getId()));
        }

        Page<ReservationResponse> page = reservationRepository.findAll(spec, pageable).map(this::toResponse);
        return PagedResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponse getById(Long id) {
        Reservation reservation = findOrThrow(id);
        assertOwnershipOrAdmin(reservation);
        return toResponse(reservation);
    }

    @Override
    @Transactional
    public ReservationResponse update(Long id, ReservationRequest request) {
        Reservation reservation = findOrThrow(id);
        assertOwnershipOrAdmin(reservation);
        validateTimeRange(request);

        Resource resource = reservation.getResource();
        if (request.getResourceId() != null && !request.getResourceId().equals(resource.getId())) {
            resource = resourceRepository.findById(request.getResourceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + request.getResourceId()));
            if (!resource.isAvailable()) {
                throw new ConflictException("Resource is not available for booking");
            }
        }

        assertNoOverlap(resource.getId(), request.getStartTime(), request.getEndTime(), reservation.getId());

        // SECURITY: reservation ownership (the user field) is never mutated
        // from client input - only resource/time/price/status are editable.
        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPrice(request.getPrice());
        if (request.getStatus() != null) {
            reservation.setStatus(request.getStatus());
        }

        return toResponse(reservationRepository.save(reservation));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Reservation reservation = findOrThrow(id);
        assertOwnershipOrAdmin(reservation);
        reservationRepository.delete(reservation);
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    private void validateTimeRange(ReservationRequest request) {
        if (request.getStartTime() != null && request.getEndTime() != null
                && !request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
    }

    private void assertNoOverlap(Long resourceId, java.time.LocalDateTime start, java.time.LocalDateTime end, Long excludeReservationId) {
        List<Reservation> overlapping = reservationRepository.findOverlapping(resourceId, start, end, excludeReservationId);
        if (!overlapping.isEmpty()) {
            throw new ConflictException("The resource is already booked for an overlapping time range");
        }
    }

    private void assertOwnershipOrAdmin(Reservation reservation) {
        if (securityUtils.isAdmin()) {
            return;
        }
        User currentUser = securityUtils.getCurrentUser();
        if (!reservation.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not have permission to access this reservation");
        }
    }

    private Reservation findOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .resourceId(reservation.getResource().getId())
                .resourceName(reservation.getResource().getName())
                .userId(reservation.getUser().getId())
                .userEmail(reservation.getUser().getEmail())
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .price(reservation.getPrice())
                .status(reservation.getStatus())
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .build();
    }
}
