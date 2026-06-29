package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateClassTestQuestionsCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateClassTestQuestionsRequest;

public final class UpdateClassTestQuestionsCommandMapper {

    private UpdateClassTestQuestionsCommandMapper() {
    }

    public static UpdateClassTestQuestionsCommand fromRequest(UUID examId, UpdateClassTestQuestionsRequest request) {
        return new UpdateClassTestQuestionsCommand(examId, request.questionIds());
    }
}
