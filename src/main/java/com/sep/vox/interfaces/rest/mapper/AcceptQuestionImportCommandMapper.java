package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.AcceptQuestionImportCommand;
import com.sep.vox.interfaces.rest.dto.request.AcceptQuestionImportRequest;

public final class AcceptQuestionImportCommandMapper {

    private AcceptQuestionImportCommandMapper() {
    }

    public static AcceptQuestionImportCommand fromRequest(UUID sessionId, AcceptQuestionImportRequest request) {
        return new AcceptQuestionImportCommand(sessionId, request.confirmedMapping());
    }
}
