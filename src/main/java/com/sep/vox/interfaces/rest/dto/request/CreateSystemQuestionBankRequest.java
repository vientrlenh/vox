package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateSystemQuestionBankCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSystemQuestionBankRequest(

    @NotNull(message = "Learning language ID is required")
    UUID learningLanguageId,

    @NotBlank(message = "Question bank name is required")
    @Size(max = 255, message = "Question bank name must not exceed 255 characters")
    String name, 

    @Size(max = 2048, message = "Question bank description must not exceed 2048 characters")
    String description
) {

    public static CreateSystemQuestionBankCommand toCommand(CreateSystemQuestionBankRequest request) {
        return new CreateSystemQuestionBankCommand(request.learningLanguageId, request.name, request.description);
    }
}
