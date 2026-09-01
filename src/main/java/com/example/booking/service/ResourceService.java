package com.example.booking.service;

import com.example.booking.dto.request.ResourceRequest;
import com.example.booking.dto.response.PagedResponse;
import com.example.booking.dto.response.ResourceResponse;
import org.springframework.data.domain.Pageable;

public interface ResourceService {
    ResourceResponse create(ResourceRequest request);
    PagedResponse<ResourceResponse> getAll(Pageable pageable);
    ResourceResponse getById(Long id);
    ResourceResponse update(Long id, ResourceRequest request);
    void delete(Long id);
}
