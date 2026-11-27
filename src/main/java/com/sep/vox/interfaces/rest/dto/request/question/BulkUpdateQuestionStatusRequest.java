package com.sep.vox.interfaces.rest.dto.request.question;

import com.sep.vox.application.port.input.command.question.BulkUpdateQuestionStatusCommand;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BulkUpdateQuestionStatusRequest(
    @NotEmpty(message = "Question list must not be blank")
    @Size(max = 200, message = "At most 200 questions can be updated at a time")
    List<UUID> questionIds,

    @NotBlank(message = "Action is required")
    @Pattern(
        regexp = "SUBMIT|APPROVE|REJECT|REQUEST_REVISION|PUBLISH|ARCHIVE|REOPEN|LOCK|UNLOCK",
        message = "Action is invalid"
    )
    String action,
    String note
) {

    public static BulkUpdateQuestionStatusCommand toCommand(BulkUpdateQuestionStatusRequest request) {
        return new BulkUpdateQuestionStatusCommand(request.questionIds(), request.action(), request.note());
    }
}
