package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateExamBlueprintSectionCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamBlueprintSectionRequest;

public final class UpdateExamBlueprintSectionCommandMapper {

    private UpdateExamBlueprintSectionCommandMapper() {
    }

    public static UpdateExamBlueprintSectionCommand fromRequest(UUID sectionId, UpdateExamBlueprintSectionRequest request) {
        return new UpdateExamBlueprintSectionCommand(
            sectionId,
            request.order(),
            request.title(),
            request.instruction(),
            request.sectionTimeLimitSeconds(),
            request.sectionWeight()
        );
    }
}
