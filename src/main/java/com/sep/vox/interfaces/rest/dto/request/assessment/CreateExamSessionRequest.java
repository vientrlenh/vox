package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateExamSessionRequest(
    @NotNull UUID examId,
    @NotNull UUID candidateId,
    @NotNull UUID paperId
) {
}
