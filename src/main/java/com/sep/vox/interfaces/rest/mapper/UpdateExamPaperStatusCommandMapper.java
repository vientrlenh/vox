package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateExamPaperStatusCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamPaperStatusRequest;

public final class UpdateExamPaperStatusCommandMapper {

    private UpdateExamPaperStatusCommandMapper() {
    }

    public static UpdateExamPaperStatusCommand fromRequest(UUID paperId, UpdateExamPaperStatusRequest request) {
        return new UpdateExamPaperStatusCommand(paperId, request.action(), request.note());
    }
}
