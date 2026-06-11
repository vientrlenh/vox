package com.sep.vox.application.port.input.command;

public record CreateQuestionEvaluationGuideCommand(
    String expectedContent,
    String keyPoints,
    String acceptableResponses,
    String offTopicExamples,
    String scoringHints,
    String commonMistakes
) {
}
