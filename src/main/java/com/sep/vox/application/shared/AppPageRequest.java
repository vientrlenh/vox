package com.sep.vox.application.shared;

public record AppPageRequest(
    int page, 
    int size
) {
    private static final int MAX_PAGE_SIZE = 50;

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
        if (size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must not be greater than 50");
        }
        return new AppPageRequest(page - 1, size);
    }
}
