package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateAssessmentBlueprintSectionItemCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateAssessmentBlueprintSectionItemRequest;

public final class CreateAssessmentBlueprintSectionCommandMapper {

    private CreateAssessmentBlueprintSectionCommandMapper() {
    }

    public static CreateAssessmentBlueprintSectionItemCommand fromRequest(UUID versionId, CreateAssessmentBlueprintSectionItemRequest request) {
        return new CreateAssessmentBlueprintSectionItemCommand(
            versionId,
            request.order(),
            request.title(),
            request.instruction(),
            request.sectionTimeLimitSeconds(),
            request.sectionWeight()
        );
    }
}
