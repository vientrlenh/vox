package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateQuestionBankCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateQuestionBankRequest;

public final class CreateQuestionBankCommandMapper {

    public static CreateQuestionBankCommand fromRequest(CreateQuestionBankRequest request) {
        return new CreateQuestionBankCommand(
            request.bankName(),
            request.description()
        );
    }
}
