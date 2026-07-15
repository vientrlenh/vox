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
import com.sep.vox.application.port.input.usecase.examsession.UpdateExamSessionStatusUseCase;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.interfaces.kafka.dto.ExamAttemptEvaluationCompletedEventDto;
import com.sep.vox.interfaces.kafka.dto.ExamAttemptEvaluationFailedEventDto;

import tools.jackson.databind.json.JsonMapper;

@Component
public class ExamAttemptEvaluationCompletedConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExamAttemptEvaluationCompletedConsumer.class);

    private final RecordExamAttemptEvaluationUseCase recordExamAttemptEvaluationUseCase;
    private final UpdateExamSessionStatusUseCase updateExamSessionStatusUseCase;
    private final ExamItemResponseRepository examItemResponseRepository;
    private final JsonMapper jsonMapper;

    public ExamAttemptEvaluationCompletedConsumer(
            RecordExamAttemptEvaluationUseCase recordExamAttemptEvaluationUseCase,
            UpdateExamSessionStatusUseCase updateExamSessionStatusUseCase,
            ExamItemResponseRepository examItemResponseRepository,
            JsonMapper jsonMapper) {
        this.recordExamAttemptEvaluationUseCase = recordExamAttemptEvaluationUseCase;
        this.updateExamSessionStatusUseCase = updateExamSessionStatusUseCase;
        this.examItemResponseRepository = examItemResponseRepository;
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
        try {
            var payload = jsonMapper.valueToTree(record.value());
            var eventType = payload.path("eventType").asText();

            if ("ExamAttemptEvaluationCompleted".equals(eventType)) {
                recordExamAttemptEvaluationUseCase.execute(
                    jsonMapper.treeToValue(payload, ExamAttemptEvaluationCompletedEventDto.class)
                );
            } else if ("ExamAttemptEvaluationFailed".equals(eventType)) {
                var failedEvent = jsonMapper.treeToValue(payload, ExamAttemptEvaluationFailedEventDto.class);
                updateExamSessionStatusUseCase.execute(new UpdateExamSessionStatusCommand(
                    UUID.fromString(failedEvent.examAttemptId()),
                    ExamSessionStatus.GRADING_FAILED
                ));
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

            updateExamSessionStatusUseCase.execute(new UpdateExamSessionStatusCommand(
                sessionId,
                ExamSessionStatus.GRADING_FAILED
            ));
        } catch (Exception ex) {
            LOGGER.error("Failed to mark exam session as GRADING_FAILED from DLT: {}", ex.getMessage(), ex);
        }
    }

    private UUID resolveSessionId(Object rawRecordValue) {
        var payload = jsonMapper.valueToTree(rawRecordValue);
        var eventType = payload.path("eventType").asText();

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
        if (responseId == null) {
            return null;
        }
        return examItemResponseRepository.findById(responseId)
            .map(response -> response.getSessionId())
            .orElse(null);
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
