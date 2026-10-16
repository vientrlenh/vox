package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateQuestionCollaboratorCommand;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateQuestionCollaboratorRequest(
    @NotNull(message = "Quyền cộng tác không được để trống")
    @Pattern(
        regexp = "READ_ONLY|CAN_USE|CAN_EDIT",
        message = "Quyền cộng tác không hợp lệ"
    )
    String permission
) {

    public static UpdateQuestionCollaboratorCommand toCommand(UUID questionId, UUID collaboratorId, UpdateQuestionCollaboratorRequest request) {
        return new UpdateQuestionCollaboratorCommand(
            questionId,
            collaboratorId,
            request.permission()
        );
    }
}
