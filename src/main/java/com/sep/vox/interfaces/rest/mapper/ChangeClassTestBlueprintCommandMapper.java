package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.ChangeClassTestBlueprintCommand;
import com.sep.vox.interfaces.rest.dto.request.ChangeClassTestBlueprintRequest;

public final class ChangeClassTestBlueprintCommandMapper {

    private ChangeClassTestBlueprintCommandMapper() {
    }

    public static ChangeClassTestBlueprintCommand fromRequest(UUID examId, ChangeClassTestBlueprintRequest request) {
        return new ChangeClassTestBlueprintCommand(examId, request.blueprintId(), request.blueprintVersionId());
    }
}
