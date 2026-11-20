package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateExamPaperItemCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamPaperItemRequest;

public final class UpdateExamPaperItemCommandMapper {

    private UpdateExamPaperItemCommandMapper() {
    }

    public static UpdateExamPaperItemCommand fromRequest(UUID paperId, UUID itemId, UpdateExamPaperItemRequest request) {
        return new UpdateExamPaperItemCommand(paperId, itemId, request.questionId());
    }
}
