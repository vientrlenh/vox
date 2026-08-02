package com.sep.vox.interfaces.kafka.dto;

public record PracticeAttemptEvaluationCompletedEventDto(
    String eventType,
    String practiceResponseId,
    PracticeAttemptEvaluationCompletedPayloadDto payload
) {
}
