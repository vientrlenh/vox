package com.sep.vox.interfaces.rest.dto.response;

public record RestApiResponse<T>(String message, T data) {

    public static <T> RestApiResponse<T> success(String message, T data) {
        return new RestApiResponse<T>(message, data);
    }

    public static <T> RestApiResponse<T> success(String message) {
        return new RestApiResponse<T>(message, null);
    }
}
