package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateExamBlueprintCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateExamBlueprintRequest;

public final class CreateExamBlueprintCommandMapper {

    private CreateExamBlueprintCommandMapper() {
    }

    public static CreateExamBlueprintCommand fromRequest(CreateExamBlueprintRequest request) {
        return new CreateExamBlueprintCommand(
            request.languageId(),
            request.schoolGradeLevelId(),
            request.code(),
            request.name(),
            request.description()
        );
    }
}
