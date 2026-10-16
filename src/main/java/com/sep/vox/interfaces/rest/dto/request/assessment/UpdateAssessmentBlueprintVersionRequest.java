package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.List;

import com.sep.vox.interfaces.rest.dto.request.AssessmentBlueprintSectionRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record UpdateAssessmentBlueprintVersionRequest(
    String description,
    Integer totalTimeLimitSeconds,
    String effectiveFrom,
    String effectiveTo,

    @Valid
    @NotEmpty(message = "Version phải có ít nhất một section")
    List<AssessmentBlueprintSectionRequest> sections
) {
}
