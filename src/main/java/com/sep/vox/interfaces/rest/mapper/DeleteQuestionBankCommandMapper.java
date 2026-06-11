package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.DeleteQuestionBankCommand;

public final class DeleteQuestionBankCommandMapper {

    private DeleteQuestionBankCommandMapper() {
    }

    public static DeleteQuestionBankCommand fromId(UUID bankId) {
        return new DeleteQuestionBankCommand(bankId);
    }
}
