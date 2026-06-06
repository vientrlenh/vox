package com.sep.vox.domain.dto;

import java.util.UUID;

public record QuestionEvaluationGuideDto(
    UUID id,
    UUID questionId,
    String expectedContent,
    String keyPoints,
    String acceptableResponses,
    String offTopicExamples,
    String scoringHints,
    String commonMistakes
) {
}
