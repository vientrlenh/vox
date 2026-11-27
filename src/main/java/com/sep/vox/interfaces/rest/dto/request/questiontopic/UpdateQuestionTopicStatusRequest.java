package com.sep.vox.interfaces.rest.dto.request.questiontopic;

import com.sep.vox.application.port.input.command.questiontopic.UpdateQuestionTopicStatusCommand;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateQuestionTopicStatusRequest(
    @NotBlank(message = "Action is required")
    @Pattern(
        regexp = "PUBLISH|ARCHIVE",
        message = "Action is invalid"
    )
    String action
) {

    public static UpdateQuestionTopicStatusCommand toCommand(UUID id, UpdateQuestionTopicStatusRequest request) {
        return new UpdateQuestionTopicStatusCommand(id, request.action());
    }
}
