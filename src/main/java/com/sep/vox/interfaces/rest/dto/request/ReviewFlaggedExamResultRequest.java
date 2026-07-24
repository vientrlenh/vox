package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ReviewFlaggedExamResultRequest(
    @NotBlank(message = "Quyết định là bắt buộc")
    @Pattern(
        regexp = "FINAL|INVALID|RETAKE_REQUIRED",
        message = "Quyết định duyệt không hợp lệ"
    )
    String decision
) {
}
