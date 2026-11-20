package com.sep.vox.interfaces.kafka.dto;

public record ExamAttemptEvaluationFailedEventDto(
    String eventType,
    Integer schemaVersion,
    String examAttemptId,
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
