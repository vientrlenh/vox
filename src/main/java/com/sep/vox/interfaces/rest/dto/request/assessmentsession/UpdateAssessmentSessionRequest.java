package com.sep.vox.interfaces.rest.dto.request.assessmentsession;

import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateAssessmentSessionRequest(
    @NotNull AssessmentSessionStatus status
) {
}
