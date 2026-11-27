package com.sep.vox.interfaces.rest.dto.request.questionbank;

import com.sep.vox.application.port.input.command.questionbank.UpdateQuestionBankCommand;
import java.util.UUID;
import jakarta.validation.constraints.Size;

public record UpdateQuestionBankRequest(
    @Size(max = 255, message = "Question bank name must not exceed 255 characters")
    String name,

    @Size(max = 2048, message = "Description must not exceed 2048 characters")
    String description
) {

    public static UpdateQuestionBankCommand toCommand(UUID id, UpdateQuestionBankRequest request) {
        return new UpdateQuestionBankCommand(
            id,
            request.name(),
            request.description()
        );
    }
}
