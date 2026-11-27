package com.sep.vox.interfaces.rest.dto.request.questionbank;

import com.sep.vox.application.port.input.command.questionbank.UpdateQuestionBankStatusCommand;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateQuestionBankStatusRequest(
    @NotBlank(message = "Action is required")
    @Pattern(
        regexp = "PUBLISH|ARCHIVE",
        message = "Action is invalid"
    )
    String action
) {

    public static UpdateQuestionBankStatusCommand toCommand(UUID id, UpdateQuestionBankStatusRequest request) {
        return new UpdateQuestionBankStatusCommand(id, request.action());
    }
}
