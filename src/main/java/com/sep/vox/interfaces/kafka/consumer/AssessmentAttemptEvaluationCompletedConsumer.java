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

import com.sep.vox.application.port.input.command.UpdateAssessmentSessionStatusCommand;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.RecordAssessmentAttemptEvaluationUseCase;
import com.sep.vox.application.port.input.usecase.assessmentitemresponse.ResolveAssessmentSessionIdByResponseIdUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.UpdateAssessmentSessionStatusUseCase;
import com.sep.vox.interfaces.kafka.dto.AssessmentAttemptEvaluationCompletedEventDto;
import com.sep.vox.interfaces.kafka.dto.AssessmentAttemptEvaluationFailedEventDto;
import com.sep.vox.interfaces.kafka.mapper.RecordAssessmentAttemptEvaluationCommandMapper;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class AssessmentAttemptEvaluationCompletedConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssessmentAttemptEvaluationCompletedConsumer.class);

    private final RecordAssessmentAttemptEvaluationUseCase recordAssessmentAttemptEvaluationUseCase;
    private final UpdateAssessmentSessionStatusUseCase updateAssessmentSessionStatusUseCase;
    private final ResolveAssessmentSessionIdByResponseIdUseCase resolveAssessmentSessionIdByResponseIdUseCase;
    private final JsonMapper jsonMapper;

    public AssessmentAttemptEvaluationCompletedConsumer(
            RecordAssessmentAttemptEvaluationUseCase recordAssessmentAttemptEvaluationUseCase,
            UpdateAssessmentSessionStatusUseCase updateAssessmentSessionStatusUseCase,
            ResolveAssessmentSessionIdByResponseIdUseCase resolveAssessmentSessionIdByResponseIdUseCase,
            JsonMapper jsonMapper) {
        this.recordAssessmentAttemptEvaluationUseCase = recordAssessmentAttemptEvaluationUseCase;
        this.updateAssessmentSessionStatusUseCase = updateAssessmentSessionStatusUseCase;
        this.resolveAssessmentSessionIdByResponseIdUseCase = resolveAssessmentSessionIdByResponseIdUseCase;
        this.jsonMapper = jsonMapper;
    }

    @RetryableTopic(
        attempts = "4",
        backOff = @BackOff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
        autoCreateTopics = "true",
        dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(
        topics = "${app.external-event.kafka.consumer-groups.assessment-evaluation.topic.assessment-attempt-evaluation-completed}",
        groupId = "${app.external-event.kafka.consumer-groups.assessment-evaluation.group-id}"
    )
    public void consume(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        // INFO-level and in the com.sep.vox namespace on purpose: org.springframework.kafka/
        // org.apache.kafka are both set to WARN in application.yaml, which silences Spring
        // Kafka's own partition-assignment/poll logs -- without a line here, a message that's
        // never delivered to this listener looks IDENTICAL in the logs to one that's delivered
        // and processed successfully (the success path below had no log statement of its own).
        LOGGER.info(
            "Received assessment evaluation event partition={} offset={} key={}",
            record.partition(), record.offset(), record.key()
        );
        try {
            JsonNode payload = jsonMapper.valueToTree(record.value());
            String eventType = payload.path("eventType").asString();

            if ("AssessmentAttemptEvaluationCompleted".equals(eventType)) {
                AssessmentAttemptEvaluationCompletedEventDto dto = jsonMapper.treeToValue(payload, AssessmentAttemptEvaluationCompletedEventDto.class);
                recordAssessmentAttemptEvaluationUseCase.execute(
                    RecordAssessmentAttemptEvaluationCommandMapper.toCommand(dto)
                );
                LOGGER.info("Recorded assessment evaluation for answerId={}", dto.answerId());
            } else if ("AssessmentAttemptEvaluationFailed".equals(eventType)) {
                AssessmentAttemptEvaluationFailedEventDto failedEvent = jsonMapper.treeToValue(payload, AssessmentAttemptEvaluationFailedEventDto.class);
                // payload is optional in the DTO: an older producer may send a bare failure event. A missing
                // payload is handled exactly like the DLT branch -- failed, reason unknown.
                AssessmentAttemptEvaluationFailedEventDto.PayloadDto failurePayload = failedEvent.payload();
                updateAssessmentSessionStatusUseCase.execute(UpdateAssessmentSessionStatusCommand.gradingFailed(
                    UUID.fromString(failedEvent.assessmentAttemptId()),
                    failurePayload == null ? null : failurePayload.error(),
                    failurePayload == null ? null : failurePayload.retryCount()
                ));
                LOGGER.warn(
                    "Assessment session {} marked GRADING_FAILED: retryCount={} error={}",
                    failedEvent.assessmentAttemptId(),
                    failurePayload == null ? null : failurePayload.retryCount(),
                    failurePayload == null ? null : failurePayload.error()
                );
            } else {
                LOGGER.info("Skip unknown assessment evaluation event type={}", eventType);
            }

            ack.acknowledge();
        } catch (Exception ex) {
            LOGGER.error("Assessment evaluation event processed failed, no ack: {}", ex.getMessage(), ex);
            throw ex instanceof RuntimeException runtimeException
                ? runtimeException
                : new IllegalStateException("Failed to process assessment evaluation event", ex);
        }
    }

    @DltHandler
    public void dltHandler(ConsumerRecord<String, Object> record) {
        LOGGER.error("Assessment evaluation DLT message at topic {}: {}", record.topic(), record.value());
        try {
            UUID sessionId = resolveSessionId(record.value());
            if (sessionId == null) {
                LOGGER.error("Assessment evaluation DLT message could not resolve sessionId: {}", record.value());
                return;
            }

            // There is NO reason to store one here, and that is the truth rather than an omission: the message
            // lands in the DLT after retries are exhausted because of a processing error on our side (sometimes
            // it cannot even be parsed), so it carries no error message from the grading service.
            updateAssessmentSessionStatusUseCase.execute(
                UpdateAssessmentSessionStatusCommand.gradingFailedWithoutReason(sessionId));
        } catch (Exception ex) {
            LOGGER.error("Failed to mark assessment session as GRADING_FAILED from DLT: {}", ex.getMessage(), ex);
        }
    }

    private UUID resolveSessionId(Object rawRecordValue) {
        JsonNode payload = jsonMapper.valueToTree(rawRecordValue);
        String eventType = payload.path("eventType").asString();

        if ("AssessmentAttemptEvaluationCompleted".equals(eventType)) {
            AssessmentAttemptEvaluationCompletedEventDto completedEvent = jsonMapper.treeToValue(payload, AssessmentAttemptEvaluationCompletedEventDto.class);
            UUID sessionId = parseUuid(completedEvent.assessmentAttemptId());
            return sessionId != null ? sessionId : resolveSessionIdFromAnswerId(completedEvent.answerId());
        }

        if ("AssessmentAttemptEvaluationFailed".equals(eventType)) {
            AssessmentAttemptEvaluationFailedEventDto failedEvent = jsonMapper.treeToValue(payload, AssessmentAttemptEvaluationFailedEventDto.class);
            UUID sessionId = parseUuid(failedEvent.assessmentAttemptId());
            return sessionId != null ? sessionId : resolveSessionIdFromAnswerId(failedEvent.answerId());
        }

        return null;
    }

    private UUID resolveSessionIdFromAnswerId(String answerId) {
        UUID responseId = parseUuid(answerId);
        return responseId == null ? null : resolveAssessmentSessionIdByResponseIdUseCase.execute(responseId);
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
