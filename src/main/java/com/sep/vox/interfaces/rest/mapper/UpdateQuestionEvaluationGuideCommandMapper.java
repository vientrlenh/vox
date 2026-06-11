package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateQuestionEvaluationGuideCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionEvaluationGuideRequest;

public final class UpdateQuestionEvaluationGuideCommandMapper {

    private UpdateQuestionEvaluationGuideCommandMapper() {
    }

    public static UpdateQuestionEvaluationGuideCommand fromRequest(
            UUID questionId,
            UpdateQuestionEvaluationGuideRequest request) {
        return new UpdateQuestionEvaluationGuideCommand(
            questionId,
            request.expectedContent(),
            request.keyPoints(),
            request.acceptableResponses(),
            request.offTopicExamples(),
            request.scoringHints(),
            request.commonMistakes()
        );
    }
}
