package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.AttachExamBlueprintCommand;
import com.sep.vox.application.port.input.command.CreateBlueprintInlineCommand;
import com.sep.vox.interfaces.rest.dto.request.AttachExamBlueprintRequest;

public final class AttachExamBlueprintCommandMapper {

    private AttachExamBlueprintCommandMapper() {
    }

    public static AttachExamBlueprintCommand fromRequest(UUID examId, AttachExamBlueprintRequest request) {
        return new AttachExamBlueprintCommand(
            examId,
            request.blueprintId(),
            request.blueprintVersionId(),
            toCommand(request.newBlueprint())
        );
    }

    private static CreateBlueprintInlineCommand toCommand(AttachExamBlueprintRequest.CreateBlueprintInlineRequest request) {
        if (request == null) {
            return null;
        }
        return new CreateBlueprintInlineCommand(
            request.code(),
            request.name(),
            request.description(),
            request.schoolGradeLevelId(),
            request.languageId()
        );
    }
}
