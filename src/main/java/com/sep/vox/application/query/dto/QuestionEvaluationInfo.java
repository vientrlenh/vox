package com.sep.vox.application.query.dto;

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
