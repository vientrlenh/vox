package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReviewQuestionRequest(
    @NotBlank(message = "Hành động không được để trống") String action,
    String note,
    String reason
) {
}
