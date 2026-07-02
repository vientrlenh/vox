package com.sep.vox.interfaces.rest.dto.request;

public record QuestionEvaluationGuideRequest(
    String expectedContent,
    String keyPoints,
    String acceptableResponses,
    String offTopicExamples,
    String scoringHints,
    String commonMistakes
) {
}
