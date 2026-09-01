package com.example.booking.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * Builds a validated Pageable from raw query parameters. Guards against
 * unbounded page sizes and arbitrary sort properties (SQL injection /
 * information-disclosure via sorting on non-allowlisted columns).
 */
public final class PageableFactory {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "startTime", "endTime", "createdAt", "price", "status", "name", "updatedAt", "id"
    );

    private static final int MAX_PAGE_SIZE = 100;

    private PageableFactory() {
    }

    public static Pageable build(Integer page, Integer size, String sortBy, String direction) {
        int safePage = (page == null || page < 0) ? 0 : page;
        int safeSize = (size == null || size <= 0) ? 10 : Math.min(size, MAX_PAGE_SIZE);

        if (sortBy == null || sortBy.isBlank()) {
            return PageRequest.of(safePage, safeSize);
        }

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sort field: " + sortBy
                    + ". Allowed values are: " + ALLOWED_SORT_FIELDS);
        }

        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(safePage, safeSize, Sort.by(dir, sortBy));
    }
}
