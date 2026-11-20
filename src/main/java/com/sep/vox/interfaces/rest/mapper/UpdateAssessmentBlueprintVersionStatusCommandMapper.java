package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateAssessmentBlueprintVersionStatusCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateAssessmentBlueprintVersionStatusRequest;

public final class UpdateAssessmentBlueprintVersionStatusCommandMapper {

    private UpdateAssessmentBlueprintVersionStatusCommandMapper() {
    }

    public static UpdateAssessmentBlueprintVersionStatusCommand fromRequest(
            UUID versionId,
            UpdateAssessmentBlueprintVersionStatusRequest request) {
        return new UpdateAssessmentBlueprintVersionStatusCommand(versionId, request.action(), request.note());
    }
}
