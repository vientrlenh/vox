package com.sep.vox.interfaces.rest.dto.request;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record CreateAssessmentBlueprintVersionRequest(
    Integer totalTimeLimitSeconds,
    Instant effectiveFrom,
    Instant effectiveTo,

    @Valid
    @NotEmpty(message = "Version phải có ít nhất một section")
    List<AssessmentBlueprintSectionRequest> sections
) {
}
