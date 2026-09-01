package com.example.booking.repository;

import com.example.booking.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    /**
     * Finds any non-cancelled reservation for the given resource whose
     * time range overlaps with [startTime, endTime). Used to enforce
     * the "no double booking" business rule. Excludes a given reservation
     * id (useful when updating an existing reservation).
     */
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.resource.id = :resourceId
              AND r.status <> com.example.booking.enums.ReservationStatus.CANCELLED
              AND (:excludeId IS NULL OR r.id <> :excludeId)
              AND r.startTime < :endTime
              AND r.endTime > :startTime
            """)
    List<Reservation> findOverlapping(@Param("resourceId") Long resourceId,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime,
                                       @Param("excludeId") Long excludeId);
}
