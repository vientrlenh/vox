package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateSystemRubricRequest(
        @NotBlank(message = "Tên bộ Rubric không được để trống")
        String name,

        String description
) {}