package com.sep.vox.interfaces.rest.dto.request.questiontopic;

import com.sep.vox.application.port.input.command.questiontopic.UpdateQuestionTopicCommand;
import java.util.UUID;
import jakarta.validation.constraints.Size;


public record UpdateQuestionTopicRequest(
    @Size(max = 255, message = "Topic name must not exceed 255 characters")
    String name,

    @Size(max = 2048, message = "Description must not exceed 2048 characters")
    String description
) {

    public static UpdateQuestionTopicCommand toCommand(UUID id, UpdateQuestionTopicRequest request) {
        return new UpdateQuestionTopicCommand(
            id,
            request.name(),
            request.description()
        );
    }
}
