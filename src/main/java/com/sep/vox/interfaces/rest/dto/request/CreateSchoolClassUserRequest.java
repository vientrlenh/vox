package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateSchoolClassUserRequest(
    @NotNull(message = "ID người dùng không được để trống")
    UUID userId
) {
}
