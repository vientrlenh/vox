package com.sep.vox.interfaces.rest.dto.request.assessment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateAssessmentMemberRequest(
    @NotBlank(message = "Role là bắt buộc")
    @Pattern(
        regexp = "CHAIR|AUTHOR|REVIEWER",
        message = "Role không hợp lệ"
    )
    String role
) {
}
