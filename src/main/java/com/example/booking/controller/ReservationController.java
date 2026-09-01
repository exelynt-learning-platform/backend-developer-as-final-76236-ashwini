package com.example.booking.controller;

import com.example.booking.dto.request.ReservationRequest;
import com.example.booking.dto.response.PagedResponse;
import com.example.booking.dto.response.ReservationResponse;
import com.example.booking.enums.ReservationStatus;
import com.example.booking.service.ReservationService;
import com.example.booking.util.PageableFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reservations", description = "Create and manage reservations")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Create a reservation. Owner is always derived from the JWT.")
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "List reservations (ADMIN sees all, USER sees only their own)")
    public ResponseEntity<PagedResponse<ReservationResponse>> getAll(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction) {
        Pageable pageable = PageableFactory.build(page, size, sortBy, direction);
        return ResponseEntity.ok(reservationService.getAll(status, minPrice, maxPrice, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Get a reservation by id (USER restricted to own reservation)")
    public ResponseEntity<ReservationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Update a reservation (USER restricted to own reservation)")
    public ResponseEntity<ReservationResponse> update(@PathVariable Long id, @Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.ok(reservationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Delete a reservation (USER restricted to own reservation)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reservationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
