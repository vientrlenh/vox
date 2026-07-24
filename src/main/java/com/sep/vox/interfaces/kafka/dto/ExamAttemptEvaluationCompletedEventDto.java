package com.sep.vox.interfaces.kafka.dto;

public record ExamAttemptEvaluationCompletedEventDto(
    String eventType,
    Integer schemaVersion,
    String examAttemptId,
    String answerId,
    String questionId,
    ExamAttemptEvaluationCompletedPayloadDto payload
) {
}
