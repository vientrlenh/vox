package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateQuestionBankStatusCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionBankStatusRequest;

public final class UpdateQuestionBankStatusCommandMapper {

    private UpdateQuestionBankStatusCommandMapper() {
    }

    public static UpdateQuestionBankStatusCommand fromRequest(UUID id, UpdateQuestionBankStatusRequest request) {
        return new UpdateQuestionBankStatusCommand(id, request.action());
    }
}
