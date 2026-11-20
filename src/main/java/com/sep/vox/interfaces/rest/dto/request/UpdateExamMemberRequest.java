package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateExamMemberRequest(
    @NotBlank(message = "Role là bắt buộc")
    @Pattern(
        regexp = "CHAIR|AUTHOR|REVIEWER",
        message = "Role không hợp lệ"
    )
    String role
) {
}
