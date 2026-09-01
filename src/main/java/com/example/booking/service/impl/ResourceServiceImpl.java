package com.example.booking.service.impl;

import com.example.booking.dto.request.ResourceRequest;
import com.example.booking.dto.response.PagedResponse;
import com.example.booking.dto.response.ResourceResponse;
import com.example.booking.entity.Resource;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;

    @Override
    @Transactional
    public ResourceResponse create(ResourceRequest request) {
        Resource resource = Resource.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .available(request.getAvailable() == null || request.getAvailable())
                .build();

        return toResponse(resourceRepository.save(resource));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ResourceResponse> getAll(Pageable pageable) {
        Page<ResourceResponse> page = resourceRepository.findAll(pageable).map(this::toResponse);
        return PagedResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public ResourceResponse update(Long id, ResourceRequest request) {
        Resource resource = findOrThrow(id);
        resource.setName(request.getName());
        resource.setDescription(request.getDescription());
        resource.setPrice(request.getPrice());
        if (request.getAvailable() != null) {
            resource.setAvailable(request.getAvailable());
        }
        return toResponse(resourceRepository.save(resource));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Resource resource = findOrThrow(id);
        resourceRepository.delete(resource);
    }

    private Resource findOrThrow(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }

    private ResourceResponse toResponse(Resource resource) {
        return ResourceResponse.builder()
                .id(resource.getId())
                .name(resource.getName())
                .description(resource.getDescription())
                .price(resource.getPrice())
                .available(resource.isAvailable())
                .createdAt(resource.getCreatedAt())
                .updatedAt(resource.getUpdatedAt())
                .build();
    }
}
