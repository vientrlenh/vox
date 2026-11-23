package com.sep.vox.application.port.input.command.question;

public record CreateQuestionEvaluationGuideCommand(
    String expectedContent,
    String keyPoints,
    String acceptableResponses,
    String offTopicExamples,
    String scoringHints,
    String commonMistakes
) {
}
