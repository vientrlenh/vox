package com.sep.vox.domain.dto;

public record ImportRowErrorDto(
        int rowNumber,
        String field,
        String message) {
}
