package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateExamSecurePoolStatusRequest(
    @NotBlank(message = "Action là bắt buộc")
    @Pattern(
        regexp = "RELEASE",
        message = "Action không hợp lệ"
    )
    String action
) {
}
