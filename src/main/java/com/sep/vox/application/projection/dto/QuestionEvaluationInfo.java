package com.sep.vox.application.projection.dto;

public record QuestionEvaluationInfo(
    String questionText,
    String evaluationGuideJson,
    String questionType,
    Integer minResponseSeconds,
    Integer maxResponseSeconds,
    String topicName,
    String topicDescription
) {
    
}
