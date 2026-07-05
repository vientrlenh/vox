package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateQuestionStatusCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionStatusRequest;

public final class UpdateQuestionStatusCommandMapper {

    private UpdateQuestionStatusCommandMapper() {
    }

    public static UpdateQuestionStatusCommand fromRequest(UUID questionId, UpdateQuestionStatusRequest request) {
        return new UpdateQuestionStatusCommand(questionId, request.action(), request.note());
    }
}
