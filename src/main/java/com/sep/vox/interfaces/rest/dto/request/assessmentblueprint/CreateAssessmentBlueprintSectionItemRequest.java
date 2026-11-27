package com.sep.vox.interfaces.rest.dto.request.assessmentblueprint;

import com.sep.vox.application.port.input.command.assessmentblueprint.CreateAssessmentBlueprintSectionItemCommand;
import java.util.UUID;
import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAssessmentBlueprintSectionItemRequest(
    @NotNull(message = "Section order is required")
    Integer order,

    @NotBlank(message = "Section title is required")
    String title,

    String instruction,
    Integer sectionTimeLimitSeconds,
    BigDecimal sectionWeight
) {

    public static CreateAssessmentBlueprintSectionItemCommand toCommand(UUID versionId, CreateAssessmentBlueprintSectionItemRequest request) {
        return new CreateAssessmentBlueprintSectionItemCommand(
            versionId,
            request.order(),
            request.title(),
            request.instruction(),
            request.sectionTimeLimitSeconds(),
            request.sectionWeight()
        );
    }
}
