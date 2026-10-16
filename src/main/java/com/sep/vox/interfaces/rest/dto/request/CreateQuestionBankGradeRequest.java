package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateQuestionBankGradeCommand;

import jakarta.validation.constraints.NotNull;

public record CreateQuestionBankGradeRequest(
    @NotNull(message = "ID khối lớp không được để trống")
    UUID schoolGradeId
) {

    public static CreateQuestionBankGradeCommand toCommand(UUID questionBankId, CreateQuestionBankGradeRequest request) {
        return new CreateQuestionBankGradeCommand(questionBankId, request.schoolGradeId());
    }
}
