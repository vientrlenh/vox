package com.sep.vox.interfaces.rest.dto.request.questiontopic;

import com.sep.vox.application.port.input.command.questiontopic.CreateQuestionTopicCommand;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateQuestionTopicRequest(
    @NotNull(message = "Question bank ID must not be blank")
    UUID questionBankId,

    @NotBlank(message = "Topic code must not be blank")
    @Size(max = 100, message = "Topic code must not exceed 100 characters")
    String code,

    @NotBlank(message = "Topic name must not be blank")
    @Size(max = 255, message = "Topic name must not exceed 255 characters")
    String name,

    @Size(max = 2048, message = "Description must not exceed 2048 characters")
    String description
) {

    public static CreateQuestionTopicCommand toCommand(CreateQuestionTopicRequest request) {
        return new CreateQuestionTopicCommand(
            request.questionBankId(),
            request.code(),
            request.name(),
            request.description()
        );
    }
}
