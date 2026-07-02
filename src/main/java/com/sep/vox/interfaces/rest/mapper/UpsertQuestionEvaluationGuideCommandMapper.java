package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpsertQuestionEvaluationGuideCommand;
import com.sep.vox.interfaces.rest.dto.request.QuestionEvaluationGuideRequest;

public final class UpsertQuestionEvaluationGuideCommandMapper {

    private UpsertQuestionEvaluationGuideCommandMapper() {
    }

    public static UpsertQuestionEvaluationGuideCommand fromRequest(
            UUID questionId,
            QuestionEvaluationGuideRequest request) {
        return new UpsertQuestionEvaluationGuideCommand(
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
