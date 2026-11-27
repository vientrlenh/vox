package com.sep.vox.interfaces.rest.dto.request.assessmentpaper;

import com.sep.vox.application.port.input.command.assessmentpaper.UpdateAssessmentPaperItemCommand;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record UpdateAssessmentPaperItemRequest(
    @NotNull(message = "QuestionId is required")
    UUID questionId
) {

    public static UpdateAssessmentPaperItemCommand toCommand(UUID paperId, UUID itemId, UpdateAssessmentPaperItemRequest request) {
        return new UpdateAssessmentPaperItemCommand(paperId, itemId, request.questionId());
    }
}
