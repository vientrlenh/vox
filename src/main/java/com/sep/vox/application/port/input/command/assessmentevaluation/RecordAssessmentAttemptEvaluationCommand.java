package com.sep.vox.application.port.input.command.assessmentevaluation;

/**
 * Input command for {@code RecordAssessmentAttemptEvaluationUseCase} -- the application layer MUST NOT
 * depend on {@code interfaces.kafka.dto.AssessmentAttemptEvaluationCompletedEventDto} (that is the wire format
 * of the Kafka adapter alone). Its shape mirrors that DTO almost 1:1 because it is exactly all the
 * data the use case needs; the child types (Payload/CriterionScore/EvaluationSignals/...) are separate
 * top-level records in this same package, with the suffix "Input" so they do not clash with the existing
 * {@code domain.valueobject.EvaluationSignals}.
 */
public record RecordAssessmentAttemptEvaluationCommand(
    String eventType,
    Integer schemaVersion,
    String assessmentAttemptId,
    String answerId,
    String questionId,
    RecordAssessmentAttemptEvaluationPayloadInput payload
) {
}
