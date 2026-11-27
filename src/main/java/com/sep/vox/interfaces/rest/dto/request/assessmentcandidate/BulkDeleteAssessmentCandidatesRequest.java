package com.sep.vox.interfaces.rest.dto.request.assessmentcandidate;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

public record BulkDeleteAssessmentCandidatesRequest(
    @NotEmpty(message = "Candidate list must not be blank")
    List<UUID> candidateIds
) {
}
