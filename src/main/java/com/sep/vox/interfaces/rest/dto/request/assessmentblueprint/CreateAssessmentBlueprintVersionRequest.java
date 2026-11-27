package com.sep.vox.interfaces.rest.dto.request.assessmentblueprint;

import com.sep.vox.application.port.input.command.assessmentblueprint.CreateAssessmentBlueprintVersionCommand;
import java.util.UUID;
import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record CreateAssessmentBlueprintVersionRequest(
    Integer totalTimeLimitSeconds,
    Instant effectiveFrom,
    Instant effectiveTo,

    @Valid
    @NotEmpty(message = "A version must have at least one section")
    List<AssessmentBlueprintSectionRequest> sections
) {

    public static CreateAssessmentBlueprintVersionCommand toCommand(UUID blueprintId, CreateAssessmentBlueprintVersionRequest request) {
        return new CreateAssessmentBlueprintVersionCommand(
            blueprintId,
            request.totalTimeLimitSeconds(),
            request.effectiveFrom(),
            request.effectiveTo(),
            AssessmentBlueprintSectionRequest.toCommands(request.sections())
        );
    }
}
