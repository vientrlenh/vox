package com.sep.vox.interfaces.rest.dto.request.assessment;

import com.sep.vox.application.port.input.command.assessment.AttachAssessmentBlueprintCommand;
import com.sep.vox.application.port.input.command.assessment.CreateBlueprintInlineCommand;
import java.util.UUID;

import jakarta.validation.Valid;

public record AttachAssessmentBlueprintRequest(
    UUID blueprintId,
    UUID blueprintVersionId,
    @Valid CreateBlueprintInlineRequest newBlueprint
) {
    public record CreateBlueprintInlineRequest(
        String code,
        String name,
        String description,
        UUID gradeLevelId,
        UUID languageId
    ) {
    }

    public static AttachAssessmentBlueprintCommand toCommand(UUID assessmentId, AttachAssessmentBlueprintRequest request) {
        return new AttachAssessmentBlueprintCommand(
            assessmentId,
            request.blueprintId(),
            request.blueprintVersionId(),
            toCommand(request.newBlueprint())
        );
    }

    private static CreateBlueprintInlineCommand toCommand(AttachAssessmentBlueprintRequest.CreateBlueprintInlineRequest request) {
        if (request == null) {
            return null;
        }
        return new CreateBlueprintInlineCommand(
            request.code(),
            request.name(),
            request.description(),
            request.gradeLevelId(),
            request.languageId()
        );
    }
}
