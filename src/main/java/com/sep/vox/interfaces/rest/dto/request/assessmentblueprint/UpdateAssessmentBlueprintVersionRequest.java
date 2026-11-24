package com.sep.vox.interfaces.rest.dto.request.assessmentblueprint;

import com.sep.vox.application.port.input.command.assessmentblueprint.UpdateAssessmentBlueprintVersionCommand;
import java.util.UUID;
import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record UpdateAssessmentBlueprintVersionRequest(
    String description,
    Integer totalTimeLimitSeconds,
    Instant effectiveFrom,
    Instant effectiveTo,

    @Valid
    @NotEmpty(message = "Version phải có ít nhất một section")
    List<AssessmentBlueprintSectionRequest> sections
) {

    public static UpdateAssessmentBlueprintVersionCommand toCommand(UUID versionId, UpdateAssessmentBlueprintVersionRequest request) {
        return new UpdateAssessmentBlueprintVersionCommand(
            versionId,
            request.description(),
            request.totalTimeLimitSeconds(),
            request.effectiveFrom(),
            request.effectiveTo(),
            AssessmentBlueprintSectionRequest.toCommands(request.sections())
        );
    }
}
