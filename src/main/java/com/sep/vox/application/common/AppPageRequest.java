package com.sep.vox.application.common;

public record AppPageRequest(
    int page, 
    int size
) {

    // Subtract by 1 for repository page queries
    public static AppPageRequest pageRequest(Integer page, Integer size) {
        if (page == null) {
            throw new IllegalArgumentException("Page number is required");
        }
        if (page <= 0) {
            throw new IllegalArgumentException("Page number must be greater than 0");
        }
        if (size == null) {
            throw new IllegalArgumentException("Page size is required");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be greater than 0");
        }
        return new AppPageRequest(page - 1, size);
    }
}
