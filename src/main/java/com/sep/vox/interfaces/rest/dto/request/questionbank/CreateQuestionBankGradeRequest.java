package com.sep.vox.interfaces.rest.dto.request.questionbank;

import com.sep.vox.application.port.input.command.questionbank.CreateQuestionBankGradeCommand;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateQuestionBankGradeRequest(
    @NotNull(message = "ID khối lớp không được để trống")
    UUID schoolGradeId
) {

    public static CreateQuestionBankGradeCommand toCommand(UUID questionBankId, CreateQuestionBankGradeRequest request) {
        return new CreateQuestionBankGradeCommand(questionBankId, request.schoolGradeId());
    }
}
