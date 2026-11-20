package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateAssessmentPaperItemCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateAssessmentPaperItemRequest;

public final class UpdateAssessmentPaperItemCommandMapper {

    private UpdateAssessmentPaperItemCommandMapper() {
    }

    public static UpdateAssessmentPaperItemCommand fromRequest(UUID paperId, UUID itemId, UpdateAssessmentPaperItemRequest request) {
        return new UpdateAssessmentPaperItemCommand(paperId, itemId, request.questionId());
    }
}
