package com.sep.vox.interfaces.rest.dto.request.assessmentcandidate;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AssessmentPaperAssignmentRequestItem(
    @NotNull(message = "Candidate is required")
    UUID candidateId,
    @NotNull(message = "Paper code is required")
    UUID paperId
) {
}
