package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateExamBlueprintSectionItemCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateExamBlueprintSectionItemRequest;

public final class CreateExamBlueprintSectionCommandMapper {

    private CreateExamBlueprintSectionCommandMapper() {
    }

    public static CreateExamBlueprintSectionItemCommand fromRequest(UUID versionId, CreateExamBlueprintSectionItemRequest request) {
        return new CreateExamBlueprintSectionItemCommand(
            versionId,
            request.order(),
            request.title(),
            request.instruction(),
            request.sectionTimeLimitSeconds(),
            request.sectionWeight()
        );
    }
}
