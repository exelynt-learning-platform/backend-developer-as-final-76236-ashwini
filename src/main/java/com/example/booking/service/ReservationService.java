package com.example.booking.service;

import com.example.booking.dto.request.ReservationRequest;
import com.example.booking.dto.response.PagedResponse;
import com.example.booking.dto.response.ReservationResponse;
import com.example.booking.enums.ReservationStatus;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ReservationService {

    ReservationResponse create(ReservationRequest request);

    PagedResponse<ReservationResponse> getAll(ReservationStatus status,
                                               BigDecimal minPrice,
                                               BigDecimal maxPrice,
                                               Pageable pageable);

    ReservationResponse getById(Long id);

    ReservationResponse update(Long id, ReservationRequest request);

    void delete(Long id);
}
