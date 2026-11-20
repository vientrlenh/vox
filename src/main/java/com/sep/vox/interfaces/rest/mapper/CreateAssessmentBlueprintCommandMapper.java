package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateAssessmentBlueprintCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateAssessmentBlueprintRequest;

public final class CreateAssessmentBlueprintCommandMapper {

    private CreateAssessmentBlueprintCommandMapper() {
    }

    public static CreateAssessmentBlueprintCommand fromRequest(CreateAssessmentBlueprintRequest request) {
        return new CreateAssessmentBlueprintCommand(
            request.languageId(),
            request.gradeLevelId(),
            request.code(),
            request.name(),
            request.description()
        );
    }
}
