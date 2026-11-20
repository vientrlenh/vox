package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateAssessmentBlueprintSectionCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateAssessmentBlueprintSectionRequest;

public final class UpdateAssessmentBlueprintSectionCommandMapper {

    private UpdateAssessmentBlueprintSectionCommandMapper() {
    }

    public static UpdateAssessmentBlueprintSectionCommand fromRequest(UUID sectionId, UpdateAssessmentBlueprintSectionRequest request) {
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
