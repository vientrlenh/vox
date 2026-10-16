package com.sep.vox.interfaces.rest.dto.request;

import java.util.Map;
import java.util.UUID;

import com.sep.vox.application.port.input.command.AcceptQuestionImportCommand;

import jakarta.validation.constraints.NotEmpty;

public record AcceptQuestionImportRequest(
    @NotEmpty(message = "Mapping import khong duoc de trong")
    Map<String, String> confirmedMapping
) {

    public static AcceptQuestionImportCommand toCommand(UUID sessionId, AcceptQuestionImportRequest request) {
        return new AcceptQuestionImportCommand(sessionId, request.confirmedMapping());
    }
}
