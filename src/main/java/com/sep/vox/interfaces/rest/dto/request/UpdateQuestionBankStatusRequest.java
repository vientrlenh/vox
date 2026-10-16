package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateQuestionBankStatusCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateQuestionBankStatusRequest(
    @NotBlank(message = "Action là bắt buộc")
    @Pattern(
        regexp = "PUBLISH|ARCHIVE",
        message = "Action không hợp lệ"
    )
    String action
) {

    public static UpdateQuestionBankStatusCommand toCommand(UUID id, UpdateQuestionBankStatusRequest request) {
        return new UpdateQuestionBankStatusCommand(id, request.action());
    }
}
