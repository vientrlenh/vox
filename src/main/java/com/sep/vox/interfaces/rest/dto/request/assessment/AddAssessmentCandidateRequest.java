package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AddAssessmentCandidateRequest(
    @NotNull(message = "Học sinh là bắt buộc")
    UUID studentId
) {
}
