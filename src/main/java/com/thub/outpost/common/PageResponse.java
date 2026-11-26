package com.thub.outpost.common;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * The paged-response envelope used by every list endpoint (see docs/STORIES.md conventions):
 * {content: [], page, size, totalElements, totalPages}.
 *
 * We return this instead of Spring's Page directly so the JSON shape is ours and stays stable.
 * An empty result is a normal 200 with content = [] and totalElements = 0, never a 404.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    /** Converts each entity on the page with the mapper and copies the paging numbers. */
    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
