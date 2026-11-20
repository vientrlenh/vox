package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateAssessmentBlueprintVersionCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateAssessmentBlueprintVersionRequest;

public final class UpdateAssessmentBlueprintVersionCommandMapper {

    private UpdateAssessmentBlueprintVersionCommandMapper() {
    }

    public static UpdateAssessmentBlueprintVersionCommand fromRequest(UUID versionId, UpdateAssessmentBlueprintVersionRequest request) {
        return new UpdateAssessmentBlueprintVersionCommand(
            versionId,
            request.description(),
            request.totalTimeLimitSeconds(),
            request.effectiveFrom(),
            request.effectiveTo(),
            CreateAssessmentBlueprintVersionCommandMapper.toSections(request.sections())
        );
    }
}
