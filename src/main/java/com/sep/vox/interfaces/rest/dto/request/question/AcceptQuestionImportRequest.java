package com.sep.vox.interfaces.rest.dto.request.question;

import com.sep.vox.application.port.input.command.question.AcceptQuestionImportCommand;
import java.util.UUID;
import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

public record AcceptQuestionImportRequest(
    @NotEmpty(message = "Import mapping must not be blank")
    Map<String, String> confirmedMapping
) {

    public static AcceptQuestionImportCommand toCommand(UUID sessionId, AcceptQuestionImportRequest request) {
        return new AcceptQuestionImportCommand(sessionId, request.confirmedMapping());
    }
}
