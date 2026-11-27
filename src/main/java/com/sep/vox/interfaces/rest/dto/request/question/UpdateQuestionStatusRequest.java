package com.sep.vox.interfaces.rest.dto.request.question;

import com.sep.vox.application.port.input.command.question.UpdateQuestionStatusCommand;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateQuestionStatusRequest(
    @NotBlank(message = "Action is required")
    @Pattern(
        regexp = "SUBMIT|APPROVE|REJECT|REQUEST_REVISION|PUBLISH|ARCHIVE|REOPEN|LOCK|UNLOCK",
        message = "Action is invalid"
    )
    String action,
    String note
) {

    public static UpdateQuestionStatusCommand toCommand(UUID questionId, UpdateQuestionStatusRequest request) {
        return new UpdateQuestionStatusCommand(questionId, request.action(), request.note());
    }
}
