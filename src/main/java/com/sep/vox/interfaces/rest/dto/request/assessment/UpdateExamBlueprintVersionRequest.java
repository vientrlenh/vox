package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.List;

import com.sep.vox.interfaces.rest.dto.request.ExamBlueprintSectionRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record UpdateExamBlueprintVersionRequest(
    String description,
    Integer totalTimeLimitSeconds,
    String effectiveFrom,
    String effectiveTo,

    @Valid
    @NotEmpty(message = "Version phải có ít nhất một section")
    List<ExamBlueprintSectionRequest> sections
) {
}
