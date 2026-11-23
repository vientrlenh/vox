package com.sep.vox.interfaces.rest.dto.request.assessmentcandidate;

import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateAssessmentCandidateStatusRequest(
    @NotNull(message = "Trạng thái là bắt buộc")
    AssessmentCandidateStatus status
) {
}
