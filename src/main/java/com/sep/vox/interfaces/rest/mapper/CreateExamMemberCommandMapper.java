package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateExamMemberCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateExamMemberRequest;

public final class CreateExamMemberCommandMapper {

    private CreateExamMemberCommandMapper() {
    }

    public static CreateExamMemberCommand fromRequest(UUID examId, CreateExamMemberRequest request) {
        return new CreateExamMemberCommand(examId, request.userId(), request.role());
    }
}
