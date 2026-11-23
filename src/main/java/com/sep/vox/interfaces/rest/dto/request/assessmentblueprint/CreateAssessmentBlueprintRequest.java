package com.sep.vox.interfaces.rest.dto.request.assessmentblueprint;

import com.sep.vox.application.port.input.command.assessmentblueprint.CreateAssessmentBlueprintCommand;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAssessmentBlueprintRequest(
    @NotNull(message = "LanguageId là bắt buộc")
    UUID languageId,

    UUID gradeLevelId,

    @NotBlank(message = "Mã blueprint là bắt buộc")
    String code,

    @NotBlank(message = "Tên blueprint là bắt buộc")
    String name,

    String description
) {

    public static CreateAssessmentBlueprintCommand toCommand(CreateAssessmentBlueprintRequest request) {
        return new CreateAssessmentBlueprintCommand(
            request.languageId(),
            request.gradeLevelId(),
            request.code(),
            request.name(),
            request.description()
        );
    }
}
