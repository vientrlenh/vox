package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateAssessmentMemberRequest(
    @NotNull(message = "UserId là bắt buộc")
    UUID userId,

    @Pattern(
        regexp = "CHAIR|AUTHOR|REVIEWER",
        message = "Role không hợp lệ"
    )
    String role
) {
}
