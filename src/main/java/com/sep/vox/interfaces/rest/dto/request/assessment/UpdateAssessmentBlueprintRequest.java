package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateAssessmentBlueprintCommand;

public record UpdateAssessmentBlueprintRequest(
    String name,
    String description
) {

    public static UpdateAssessmentBlueprintCommand toCommand(UUID blueprintId, UpdateAssessmentBlueprintRequest request) {
        return new UpdateAssessmentBlueprintCommand(blueprintId, request.name(), request.description());
    }
}
