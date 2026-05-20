package com.booker.shared.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Stable JSON envelope for paginated results.
 * Replaces direct serialization of {@link org.springframework.data.domain.PageImpl},
 * which Spring Data warns is not guaranteed to be stable across versions.
 */
public record PagedResponse<T>(
        List<T> content,
        int number,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> PagedResponse<T> of(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
