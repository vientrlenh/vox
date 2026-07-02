package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateExamStatusCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamStatusRequest;

public final class UpdateExamStatusCommandMapper {

    private UpdateExamStatusCommandMapper() {
    }

    public static UpdateExamStatusCommand fromRequest(UUID examId, UpdateExamStatusRequest request) {
        return new UpdateExamStatusCommand(examId, request.action(), request.note());
    }
}
