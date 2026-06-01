package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateQuestionBankCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionBankRequest;

public final class UpdateQuestionBankCommandMapper {

    public static UpdateQuestionBankCommand fromRequest(UUID id, UpdateQuestionBankRequest request) {
        return new UpdateQuestionBankCommand(
            id,
            request.bankName(),
            request.description(),
            request.isActive()
        );
    }
}
