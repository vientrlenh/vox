package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import com.sep.vox.application.port.input.command.ChangeClassTestBlueprintCommand;

public record ChangeClassTestBlueprintRequest(
    UUID blueprintId,
    UUID blueprintVersionId
) {

    public static ChangeClassTestBlueprintCommand toCommand(UUID assessmentId, ChangeClassTestBlueprintRequest request) {
        return new ChangeClassTestBlueprintCommand(assessmentId, request.blueprintId(), request.blueprintVersionId());
    }
}
