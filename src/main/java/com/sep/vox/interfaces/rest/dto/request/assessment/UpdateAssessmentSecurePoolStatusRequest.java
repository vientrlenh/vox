package com.sep.vox.interfaces.rest.dto.request.assessment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateAssessmentSecurePoolStatusRequest(
    @NotBlank(message = "Action là bắt buộc")
    @Pattern(
        regexp = "RELEASE",
        message = "Action không hợp lệ"
    )
    String action
) {
}
