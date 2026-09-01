package com.example.booking.controller;

import com.example.booking.dto.request.ResourceRequest;
import com.example.booking.dto.response.PagedResponse;
import com.example.booking.dto.response.ResourceResponse;
import com.example.booking.service.ResourceService;
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

@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Resources", description = "Manage bookable resources")
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a resource (ADMIN only)")
    public ResponseEntity<ResourceResponse> create(@Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resourceService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "List resources (paginated)")
    public ResponseEntity<PagedResponse<ResourceResponse>> getAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction) {
        Pageable pageable = PageableFactory.build(page, size, sortBy, direction);
        return ResponseEntity.ok(resourceService.getAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Get a resource by id")
    public ResponseEntity<ResourceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(resourceService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a resource (ADMIN only)")
    public ResponseEntity<ResourceResponse> update(@PathVariable Long id, @Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.ok(resourceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a resource (ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        resourceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
