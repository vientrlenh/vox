package com.sep.vox.interfaces.rest.dto.request.assessmentblueprint;

import com.sep.vox.application.port.input.command.assessmentblueprint.UpdateAssessmentBlueprintCommand;
import java.util.UUID;

public record UpdateAssessmentBlueprintRequest(
    String name,
    String description
) {

    public static UpdateAssessmentBlueprintCommand toCommand(UUID blueprintId, UpdateAssessmentBlueprintRequest request) {
        return new UpdateAssessmentBlueprintCommand(blueprintId, request.name(), request.description());
    }
}
