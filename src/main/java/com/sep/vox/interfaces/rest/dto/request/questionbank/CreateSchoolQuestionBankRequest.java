package com.sep.vox.interfaces.rest.dto.request.questionbank;

import com.sep.vox.application.port.input.command.questionbank.CreateSchoolQuestionBankCommand;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSchoolQuestionBankRequest(
    @NotNull(message = "Language ID must not be blank")
    UUID languageId, 

    @NotBlank(message = "Question bank code must not be blank")
    @Size(max = 100, message = "Question bank code must not exceed 100 characters")
    String code, 

    @NotBlank(message = "Question bank name must not be blank")
    @Size(max = 255, message = "Question bank name must not exceed 255 characters")
    String name, 

    @Size(max = 2048, message = "Question bank description must not exceed 2048 characters")
    String description
) {

    public static CreateSchoolQuestionBankCommand toCommand(CreateSchoolQuestionBankRequest request) {
        return new CreateSchoolQuestionBankCommand(
            request.languageId(),
            null,
            request.code(),
            request.name(),
            request.description()
        );
    }
}
