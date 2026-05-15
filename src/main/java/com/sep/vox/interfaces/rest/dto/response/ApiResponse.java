package com.sep.vox.interfaces.rest.dto.response;

public record ApiResponse<T>(String message, T data) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<T>(message, data);
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<T>(message, null);
    }
}
