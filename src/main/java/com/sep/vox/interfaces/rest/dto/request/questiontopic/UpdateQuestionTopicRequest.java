package com.sep.vox.interfaces.rest.dto.request.questiontopic;

import com.sep.vox.application.port.input.command.questiontopic.UpdateQuestionTopicCommand;
import java.util.UUID;
import jakarta.validation.constraints.Size;


public record UpdateQuestionTopicRequest(
    @Size(max = 255, message = "Tên chủ đề không được vượt quá 255 ký tự")
    String name,

    @Size(max = 2048, message = "Mô tả không được vượt quá 2048 ký tự")
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
