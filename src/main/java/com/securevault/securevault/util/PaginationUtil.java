package com.securevault.securevault.util;

import com.securevault.securevault.common.MessageKeys;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Locale;

public final class PaginationUtil {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private PaginationUtil() {
    }

    public static Pageable createPageable(
            Integer page,
            Integer size,
            String sortBy,
            String direction,
            String defaultSortBy,
            MessageSource messageSource) {

        int pageNumber = page == null ? DEFAULT_PAGE : page;
        int pageSize = size == null ? DEFAULT_SIZE : size;

        if (pageNumber < 0) {
            throw new IllegalArgumentException(
                    messageSource.getMessage(
                            MessageKeys.PAGINATION_PAGE_INVALID,
                            null,
                            Locale.getDefault()));
        }

        if (pageSize < 1 || pageSize > MAX_SIZE) {
            throw new IllegalArgumentException(
                    messageSource.getMessage(
                            MessageKeys.PAGINATION_SIZE_INVALID,
                            null,
                            Locale.getDefault()));
        }

        String sortField = sortBy == null || sortBy.isBlank()
                ? defaultSortBy
                : sortBy.trim();

        Sort.Direction sortDirection;

        try {
            sortDirection = direction == null || direction.isBlank()
                    ? Sort.Direction.DESC
                    : Sort.Direction.fromString(
                            direction.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    messageSource.getMessage(
                            MessageKeys.PAGINATION_SORT_INVALID_DIRECTION,
                            null,
                            Locale.getDefault()));
        }

        Sort sort = Sort.by(sortDirection, sortField)
                .and(Sort.by(Sort.Direction.ASC, "id"));

        return PageRequest.of(pageNumber, pageSize, sort);
    }
}