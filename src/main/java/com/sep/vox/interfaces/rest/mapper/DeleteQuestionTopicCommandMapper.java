package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.DeleteQuestionTopicCommand;

public final class DeleteQuestionTopicCommandMapper {

    private DeleteQuestionTopicCommandMapper() {
    }

    public static DeleteQuestionTopicCommand fromId(UUID topicId) {
        return new DeleteQuestionTopicCommand(topicId);
    }
}
