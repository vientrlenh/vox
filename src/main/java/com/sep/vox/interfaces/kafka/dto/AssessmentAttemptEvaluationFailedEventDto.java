package com.sep.vox.interfaces.kafka.dto;

public record AssessmentAttemptEvaluationFailedEventDto(
    String eventType,
    Integer schemaVersion,
    String assessmentAttemptId,
    String answerId,
    String questionId,
    PayloadDto payload
) {
    public record PayloadDto(
        String error,
        Integer retryCount
    ) {
    }
}
