package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SessionReasonRequest(
    @NotBlank(message = "Lý do là bắt buộc")
    String reason
) {
}
