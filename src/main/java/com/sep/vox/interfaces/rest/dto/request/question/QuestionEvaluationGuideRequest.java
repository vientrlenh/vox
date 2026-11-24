package com.sep.vox.interfaces.rest.dto.request.question;

import com.sep.vox.application.port.input.command.question.UpsertQuestionEvaluationGuideCommand;
import java.util.UUID;

public record QuestionEvaluationGuideRequest(
    String expectedContent,
    String keyPoints,
    String acceptableResponses,
    String offTopicExamples,
    String scoringHints,
    String commonMistakes
) {

    public static UpsertQuestionEvaluationGuideCommand toCommand(
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
