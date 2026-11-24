package com.sep.vox.interfaces.rest.dto.request.assessment;

import com.sep.vox.application.port.input.command.assessment.ChangeClassTestBlueprintCommand;
import java.util.UUID;

public record ChangeClassTestBlueprintRequest(
    UUID blueprintId,
    UUID blueprintVersionId
) {

    public static ChangeClassTestBlueprintCommand toCommand(UUID assessmentId, ChangeClassTestBlueprintRequest request) {
        return new ChangeClassTestBlueprintCommand(assessmentId, request.blueprintId(), request.blueprintVersionId());
    }
}
