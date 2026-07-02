package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateQuestionTopicCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateQuestionTopicRequest;

public final class CreateQuestionTopicCommandMapper {

    public static CreateQuestionTopicCommand fromRequest(CreateQuestionTopicRequest request) {
        return new CreateQuestionTopicCommand(
            request.questionBankId(),
            request.code(),
            request.name(),
            request.description()
        );
    }
}
