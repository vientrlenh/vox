package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateExamBlueprintVersionCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamBlueprintVersionRequest;

public final class UpdateExamBlueprintVersionCommandMapper {

    private UpdateExamBlueprintVersionCommandMapper() {
    }

    public static UpdateExamBlueprintVersionCommand fromRequest(UUID versionId, UpdateExamBlueprintVersionRequest request) {
        return new UpdateExamBlueprintVersionCommand(
            versionId,
            request.description(),
            request.totalTimeLimitSeconds(),
            request.effectiveFrom(),
            request.effectiveTo(),
            CreateExamBlueprintVersionCommandMapper.toSections(request.sections())
        );
    }
}
