package com.example.booking.repository;

import com.example.booking.entity.Reservation;
import com.example.booking.enums.ReservationStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * Builds composable JPA Specifications for filtering reservations,
 * avoiding a proliferation of ad-hoc repository query methods.
 */
public final class ReservationSpecifications {

    private ReservationSpecifications() {
    }

    public static Specification<Reservation> hasStatus(ReservationStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Reservation> minPrice(BigDecimal minPrice) {
        return (root, query, cb) -> minPrice == null ? null : cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Reservation> maxPrice(BigDecimal maxPrice) {
        return (root, query, cb) -> maxPrice == null ? null : cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    public static Specification<Reservation> belongsToUser(Long userId) {
        return (root, query, cb) -> userId == null ? null : cb.equal(root.get("user").get("id"), userId);
    }
}
