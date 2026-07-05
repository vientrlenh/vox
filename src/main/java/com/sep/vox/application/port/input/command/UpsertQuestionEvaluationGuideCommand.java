package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpsertQuestionEvaluationGuideCommand(
    UUID questionId,
    String expectedContent,
    String keyPoints,
    String acceptableResponses,
    String offTopicExamples,
    String scoringHints,
    String commonMistakes
) {
}
