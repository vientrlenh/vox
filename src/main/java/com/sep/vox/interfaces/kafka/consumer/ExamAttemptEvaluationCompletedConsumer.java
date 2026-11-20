package com.sep.vox.interfaces.kafka.consumer;

import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.command.UpdateExamSessionStatusCommand;
import com.sep.vox.application.port.input.usecase.examevaluation.RecordExamAttemptEvaluationUseCase;
import com.sep.vox.application.port.input.usecase.examitemresponse.ResolveExamSessionIdByResponseIdUseCase;
import com.sep.vox.application.port.input.usecase.examsession.UpdateExamSessionStatusUseCase;
import com.sep.vox.interfaces.kafka.dto.ExamAttemptEvaluationCompletedEventDto;
import com.sep.vox.interfaces.kafka.dto.ExamAttemptEvaluationFailedEventDto;
import com.sep.vox.interfaces.kafka.mapper.RecordExamAttemptEvaluationCommandMapper;

import tools.jackson.databind.json.JsonMapper;

@Component
public class ExamAttemptEvaluationCompletedConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExamAttemptEvaluationCompletedConsumer.class);

    private final RecordExamAttemptEvaluationUseCase recordExamAttemptEvaluationUseCase;
    private final UpdateExamSessionStatusUseCase updateExamSessionStatusUseCase;
    private final ResolveExamSessionIdByResponseIdUseCase resolveExamSessionIdByResponseIdUseCase;
    private final JsonMapper jsonMapper;

    public ExamAttemptEvaluationCompletedConsumer(
            RecordExamAttemptEvaluationUseCase recordExamAttemptEvaluationUseCase,
            UpdateExamSessionStatusUseCase updateExamSessionStatusUseCase,
            ResolveExamSessionIdByResponseIdUseCase resolveExamSessionIdByResponseIdUseCase,
            JsonMapper jsonMapper) {
        this.recordExamAttemptEvaluationUseCase = recordExamAttemptEvaluationUseCase;
        this.updateExamSessionStatusUseCase = updateExamSessionStatusUseCase;
        this.resolveExamSessionIdByResponseIdUseCase = resolveExamSessionIdByResponseIdUseCase;
        this.jsonMapper = jsonMapper;
    }

    @RetryableTopic(
        attempts = "4",
        backOff = @BackOff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
        autoCreateTopics = "true",
        dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(
        topics = "${app.external-event.kafka.consumer-groups.exam-evaluation.topic.exam-attempt-evaluation-completed}",
        groupId = "${app.external-event.kafka.consumer-groups.exam-evaluation.group-id}"
    )
    public void consume(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        // INFO-level and in the com.sep.vox namespace on purpose: org.springframework.kafka/
        // org.apache.kafka are both set to WARN in application.yaml, which silences Spring
        // Kafka's own partition-assignment/poll logs -- without a line here, a message that's
        // never delivered to this listener looks IDENTICAL in the logs to one that's delivered
        // and processed successfully (the success path below had no log statement of its own).
        LOGGER.info(
            "Received exam evaluation event partition={} offset={} key={}",
            record.partition(), record.offset(), record.key()
        );
        try {
            var payload = jsonMapper.valueToTree(record.value());
            var eventType = payload.path("eventType").asString();

            if ("ExamAttemptEvaluationCompleted".equals(eventType)) {
                var dto = jsonMapper.treeToValue(payload, ExamAttemptEvaluationCompletedEventDto.class);
                recordExamAttemptEvaluationUseCase.execute(
                    RecordExamAttemptEvaluationCommandMapper.toCommand(dto)
                );
                LOGGER.info("Recorded exam evaluation for answerId={}", dto.answerId());
            } else if ("ExamAttemptEvaluationFailed".equals(eventType)) {
                var failedEvent = jsonMapper.treeToValue(payload, ExamAttemptEvaluationFailedEventDto.class);
                // payload là optional trong DTO: một producer cũ hơn có thể gửi sự kiện lỗi trần.
                // Thiếu payload xử lý y hệt nhánh DLT — hỏng, không rõ vì sao.
                var failurePayload = failedEvent.payload();
                updateExamSessionStatusUseCase.execute(UpdateExamSessionStatusCommand.gradingFailed(
                    UUID.fromString(failedEvent.examAttemptId()),
                    failurePayload == null ? null : failurePayload.error(),
                    failurePayload == null ? null : failurePayload.retryCount()
                ));
                LOGGER.warn(
                    "Exam session {} marked GRADING_FAILED: retryCount={} error={}",
                    failedEvent.examAttemptId(),
                    failurePayload == null ? null : failurePayload.retryCount(),
                    failurePayload == null ? null : failurePayload.error()
                );
            } else {
                LOGGER.info("Skip unknown exam evaluation event type={}", eventType);
            }

            ack.acknowledge();
        } catch (Exception ex) {
            LOGGER.error("Exam evaluation event processed failed, no ack: {}", ex.getMessage(), ex);
            throw ex instanceof RuntimeException runtimeException
                ? runtimeException
                : new IllegalStateException("Failed to process exam evaluation event", ex);
        }
    }

    @DltHandler
    public void dltHandler(ConsumerRecord<String, Object> record) {
        LOGGER.error("Exam evaluation DLT message at topic {}: {}", record.topic(), record.value());
        try {
            var sessionId = resolveSessionId(record.value());
            if (sessionId == null) {
                LOGGER.error("Exam evaluation DLT message could not resolve sessionId: {}", record.value());
                return;
            }

            // KHÔNG có lý do để lưu ở đây, và đó là sự thật chứ không phải thiếu sót: bản tin rơi
            // xuống DLT sau khi hết lượt retry vì một lỗi xử lý phía mình (có khi còn không parse
            // được), nên nó không mang thông điệp lỗi nào của dịch vụ chấm.
            updateExamSessionStatusUseCase.execute(
                UpdateExamSessionStatusCommand.gradingFailedWithoutReason(sessionId));
        } catch (Exception ex) {
            LOGGER.error("Failed to mark exam session as GRADING_FAILED from DLT: {}", ex.getMessage(), ex);
        }
    }

    private UUID resolveSessionId(Object rawRecordValue) {
        var payload = jsonMapper.valueToTree(rawRecordValue);
        var eventType = payload.path("eventType").asString();

        if ("ExamAttemptEvaluationCompleted".equals(eventType)) {
            var completedEvent = jsonMapper.treeToValue(payload, ExamAttemptEvaluationCompletedEventDto.class);
            var sessionId = parseUuid(completedEvent.examAttemptId());
            return sessionId != null ? sessionId : resolveSessionIdFromAnswerId(completedEvent.answerId());
        }

        if ("ExamAttemptEvaluationFailed".equals(eventType)) {
            var failedEvent = jsonMapper.treeToValue(payload, ExamAttemptEvaluationFailedEventDto.class);
            var sessionId = parseUuid(failedEvent.examAttemptId());
            return sessionId != null ? sessionId : resolveSessionIdFromAnswerId(failedEvent.answerId());
        }

        return null;
    }

    private UUID resolveSessionIdFromAnswerId(String answerId) {
        var responseId = parseUuid(answerId);
        return responseId == null ? null : resolveExamSessionIdByResponseIdUseCase.execute(responseId);
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
