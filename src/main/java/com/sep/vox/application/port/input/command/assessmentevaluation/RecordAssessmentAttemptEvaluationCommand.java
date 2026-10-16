package com.sep.vox.application.port.input.command.assessmentevaluation;

/**
 * Input command cho {@code RecordAssessmentAttemptEvaluationUseCase} -- application layer KHÔNG được
 * phụ thuộc {@code interfaces.kafka.dto.AssessmentAttemptEvaluationCompletedEventDto} (đó là định dạng
 * wire-format của riêng Kafka adapter). Shape mirror gần như 1:1 với DTO đó vì đây chính là toàn
 * bộ dữ liệu use case cần; các type con (Payload/CriterionScore/EvaluationSignals/...) là các
 * record top-level riêng trong cùng package này, hậu tố "Input" để không trùng tên với
 * {@code domain.valueobject.EvaluationSignals} đã tồn tại.
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
