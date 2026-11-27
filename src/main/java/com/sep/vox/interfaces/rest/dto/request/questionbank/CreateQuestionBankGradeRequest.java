package com.sep.vox.interfaces.rest.dto.request.questionbank;

import com.sep.vox.application.port.input.command.questionbank.CreateQuestionBankGradeCommand;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateQuestionBankGradeRequest(
    @NotNull(message = "Grade level ID must not be blank")
    UUID schoolGradeId
) {

    public static CreateQuestionBankGradeCommand toCommand(UUID questionBankId, CreateQuestionBankGradeRequest request) {
        return new CreateQuestionBankGradeCommand(questionBankId, request.schoolGradeId());
    }
}
