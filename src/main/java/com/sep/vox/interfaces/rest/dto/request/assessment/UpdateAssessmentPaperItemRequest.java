package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateAssessmentPaperItemCommand;

import jakarta.validation.constraints.NotNull;

public record UpdateAssessmentPaperItemRequest(
    @NotNull(message = "QuestionId là bắt buộc")
    UUID questionId
) {

    public static UpdateAssessmentPaperItemCommand toCommand(UUID paperId, UUID itemId, UpdateAssessmentPaperItemRequest request) {
        return new UpdateAssessmentPaperItemCommand(paperId, itemId, request.questionId());
    }
}
