package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateExamBlueprintVersionStatusCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamBlueprintVersionStatusRequest;

public final class UpdateExamBlueprintVersionStatusCommandMapper {

    private UpdateExamBlueprintVersionStatusCommandMapper() {
    }

    public static UpdateExamBlueprintVersionStatusCommand fromRequest(
            UUID versionId,
            UpdateExamBlueprintVersionStatusRequest request) {
        return new UpdateExamBlueprintVersionStatusCommand(versionId, request.action(), request.note());
    }
}
