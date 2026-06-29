package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateExamSecurePoolStatusCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamSecurePoolStatusRequest;

public final class UpdateExamSecurePoolStatusCommandMapper {

    private UpdateExamSecurePoolStatusCommandMapper() {
    }

    public static UpdateExamSecurePoolStatusCommand fromRequest(UUID examId, UpdateExamSecurePoolStatusRequest request) {
        return new UpdateExamSecurePoolStatusCommand(examId, request.action());
    }
}
