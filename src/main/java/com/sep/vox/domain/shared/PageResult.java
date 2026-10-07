package com.sep.vox.domain.shared;

import java.util.List;

public record PageResult<T> (
    List<T> content,
    int page,
    int size, 
    long totalElements,
    int totalPages
) {
    public static <T> PageResult<T> fromRepository(List<T> content, int page, int size, long totalElements, int totalPages) {
        return new PageResult<T>(
            content, 
            page + 1, 
            size, 
            totalElements, 
            totalPages
        );
    }

    public static <T> PageResult<T> pageResult(List<T> content, int page, int size, long totalElements, int totalPages) {
        return new PageResult<T>(
            content, 
            page, 
            size, 
            totalElements, 
            totalPages
        );
    }
}
