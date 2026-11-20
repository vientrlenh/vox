package com.sep.vox.application.port.input.command.examevaluation;

/**
 * Input command cho {@code RecordExamAttemptEvaluationUseCase} -- application layer KHÔNG được
 * phụ thuộc {@code interfaces.kafka.dto.ExamAttemptEvaluationCompletedEventDto} (đó là định dạng
 * wire-format của riêng Kafka adapter). Shape mirror gần như 1:1 với DTO đó vì đây chính là toàn
 * bộ dữ liệu use case cần; các type con (Payload/CriterionScore/EvaluationSignals/...) là các
 * record top-level riêng trong cùng package này, hậu tố "Input" để không trùng tên với
 * {@code domain.valueobject.EvaluationSignals} đã tồn tại.
 */
public record RecordExamAttemptEvaluationCommand(
    String eventType,
    Integer schemaVersion,
    String examAttemptId,
    String answerId,
    String questionId,
    RecordExamAttemptEvaluationPayloadInput payload
) {
}
