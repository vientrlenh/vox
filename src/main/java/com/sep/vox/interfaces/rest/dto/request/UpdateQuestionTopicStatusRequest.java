package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateQuestionTopicStatusCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateQuestionTopicStatusRequest(
    @NotBlank(message = "Action là bắt buộc")
    @Pattern(
        regexp = "PUBLISH|ARCHIVE",
        message = "Action không hợp lệ"
    )
    String action
) {

    public static UpdateQuestionTopicStatusCommand toCommand(UUID id, UpdateQuestionTopicStatusRequest request) {
        return new UpdateQuestionTopicStatusCommand(id, request.action());
    }
}
