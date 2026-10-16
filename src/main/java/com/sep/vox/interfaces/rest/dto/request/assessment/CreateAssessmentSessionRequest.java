package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateAssessmentSessionRequest(
    @NotNull UUID assessmentId,
    @NotNull UUID candidateId,
    @NotNull UUID paperId
) {
}
