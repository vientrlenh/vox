package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateExamMemberCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamMemberRequest;

public final class UpdateExamMemberCommandMapper {

    private UpdateExamMemberCommandMapper() {
    }

    public static UpdateExamMemberCommand fromRequest(UUID examId, UUID memberId, UpdateExamMemberRequest request) {
        return new UpdateExamMemberCommand(examId, memberId, request.role());
    }
}
