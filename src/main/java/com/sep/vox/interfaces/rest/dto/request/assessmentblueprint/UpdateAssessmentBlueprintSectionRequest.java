package com.sep.vox.interfaces.rest.dto.request.assessmentblueprint;

import com.sep.vox.application.port.input.command.assessmentblueprint.UpdateAssessmentBlueprintSectionCommand;
import java.util.UUID;
import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateAssessmentBlueprintSectionRequest(
    @NotNull(message = "Section order is required")
    Integer order,

    @NotBlank(message = "Section title is required")
    String title,

    String instruction,
    Integer sectionTimeLimitSeconds,
    BigDecimal sectionWeight
) {

    public static UpdateAssessmentBlueprintSectionCommand toCommand(UUID sectionId, UpdateAssessmentBlueprintSectionRequest request) {
        return new UpdateAssessmentBlueprintSectionCommand(
            sectionId,
            request.order(),
            request.title(),
            request.instruction(),
            request.sectionTimeLimitSeconds(),
            request.sectionWeight()
        );
    }
}
