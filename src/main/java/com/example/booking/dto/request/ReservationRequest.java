package com.example.booking.dto.request;

import com.example.booking.enums.ReservationStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Note: intentionally does NOT contain a userId field.
 * The reservation owner is always resolved from the authenticated
 * JWT principal on the server side - never trusted from client input.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {

    @NotNull(message = "resourceId is required")
    private Long resourceId;

    @NotNull(message = "startTime is required")
    private LocalDateTime startTime;

    @NotNull(message = "endTime is required")
    private LocalDateTime endTime;

    @NotNull(message = "price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be greater than or equal to zero")
    private BigDecimal price;

    /**
     * Optional on create (defaults to PENDING). Allowed on update by ADMIN/owner.
     */
    private ReservationStatus status;
}
