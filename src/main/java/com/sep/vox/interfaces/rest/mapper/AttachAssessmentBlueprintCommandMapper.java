package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.AttachAssessmentBlueprintCommand;
import com.sep.vox.application.port.input.command.CreateBlueprintInlineCommand;
import com.sep.vox.interfaces.rest.dto.request.AttachAssessmentBlueprintRequest;

public final class AttachAssessmentBlueprintCommandMapper {

    private AttachAssessmentBlueprintCommandMapper() {
    }

    public static AttachAssessmentBlueprintCommand fromRequest(UUID assessmentId, AttachAssessmentBlueprintRequest request) {
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
