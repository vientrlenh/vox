package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateExamBlueprintCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamBlueprintRequest;

public final class UpdateExamBlueprintCommandMapper {

    private UpdateExamBlueprintCommandMapper() {
    }

    public static UpdateExamBlueprintCommand fromRequest(UUID blueprintId, UpdateExamBlueprintRequest request) {
        return new UpdateExamBlueprintCommand(blueprintId, request.name(), request.description());
    }
}
