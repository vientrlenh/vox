package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentMemberRole;

import jakarta.validation.constraints.NotNull;

public record CreateAssessmentMemberRequest(
    @NotNull(message = "UserId là bắt buộc")
    UUID userId,

    @NotNull(message = "Role là bắt buộc")
    AssessmentMemberRole role
) {
}
