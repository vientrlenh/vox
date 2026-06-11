package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.DeleteQuestionCommand;

public final class DeleteQuestionCommandMapper {

    private DeleteQuestionCommandMapper() {
    }

    public static DeleteQuestionCommand fromId(UUID questionId) {
        return new DeleteQuestionCommand(questionId);
    }
}
