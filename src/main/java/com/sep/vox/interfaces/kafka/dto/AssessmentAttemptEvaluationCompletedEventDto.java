package com.sep.vox.interfaces.kafka.dto;

public record AssessmentAttemptEvaluationCompletedEventDto(
    String eventType,
    Integer schemaVersion,
    String assessmentAttemptId,
    String answerId,
    String questionId,
    AssessmentAttemptEvaluationCompletedPayloadDto payload
) {
}
