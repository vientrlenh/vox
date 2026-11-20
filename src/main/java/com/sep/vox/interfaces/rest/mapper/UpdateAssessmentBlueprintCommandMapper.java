package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateAssessmentBlueprintCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateAssessmentBlueprintRequest;

public final class UpdateAssessmentBlueprintCommandMapper {

    private UpdateAssessmentBlueprintCommandMapper() {
    }

    public static UpdateAssessmentBlueprintCommand fromRequest(UUID blueprintId, UpdateAssessmentBlueprintRequest request) {
        return new UpdateAssessmentBlueprintCommand(blueprintId, request.name(), request.description());
    }
}
