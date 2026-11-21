package com.sep.vox.interfaces.rest.dto.request;

import com.sep.vox.domain.model.assessment.AssessmentMemberRole;

import jakarta.validation.constraints.NotNull;

public record UpdateAssessmentMemberRequest(
    @NotNull(message = "Role là bắt buộc")
    AssessmentMemberRole role
) {
}
