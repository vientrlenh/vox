package com.sep.vox.interfaces.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.usecase.practiceevaluation.RecordPracticeAttemptEvaluationUseCase;
import java.util.UUID;

import com.sep.vox.domain.repository.PracticeItemResponseRepository;
import com.sep.vox.interfaces.kafka.dto.PracticeAttemptEvaluationCompletedEventDto;
import com.sep.vox.interfaces.kafka.dto.PracticeAttemptEvaluationFailedEventDto;
import com.sep.vox.interfaces.kafka.mapper.RecordPracticeAttemptEvaluationCommandMapper;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class PracticeAttemptEvaluationCompletedConsumer {

    private final RecordPracticeAttemptEvaluationUseCase recordPracticeAttemptEvaluationUseCase;
    private final PracticeItemResponseRepository practiceItemResponseRepository;
    private final JsonMapper jsonMapper;

    @RetryableTopic(
        attempts = "4",
        backOff = @BackOff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
        autoCreateTopics = "true",
        dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(
        topics = "${app.external-event.kafka.consumer-groups.practice-evaluation.topic.practice-attempt-evaluation-completed}",
        groupId = "${app.external-event.kafka.consumer-groups.practice-evaluation.group-id}"
    )
    public void consume(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        log.info(
            "Received practice evaluation event partition={} offset={} key={}",
            record.partition(), record.offset(), record.key()
        );
        try {
            processEvent(jsonMapper.valueToTree(record.value()));

            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Practice evaluation event processed failed, no ack: {}", ex.getMessage(), ex);
            throw ex instanceof RuntimeException runtimeException
                ? runtimeException
                : new IllegalStateException("Failed to process practice evaluation event", ex);
        }
    }

    private void processEvent(JsonNode payload) {
        String eventType = payload.path("eventType").asString();

        if ("PracticeAttemptEvaluationCompleted".equals(eventType)) {
            recordCompletedEvaluation(payload);
        } else if ("PracticeAttemptEvaluationFailed".equals(eventType)) {
            markGradingFailed(payload);
        } else {
            log.info("Skip unknown practice evaluation event type={}", eventType);
        }
    }

    private void recordCompletedEvaluation(JsonNode payload) {
        PracticeAttemptEvaluationCompletedEventDto dto = jsonMapper.treeToValue(payload, PracticeAttemptEvaluationCompletedEventDto.class);
        recordPracticeAttemptEvaluationUseCase.execute(
            RecordPracticeAttemptEvaluationCommandMapper.toCommand(dto)
        );
        log.info("Recorded practice evaluation for practiceResponseId={}", dto.practiceResponseId());
    }

    private void markGradingFailed(JsonNode payload) {
        // Before 2026-08-12 this branch did not exist: a failed event fell into "Skip unknown", so Java
        // never learned that grading had failed. That question stayed "no grading result" forever, the
        // sweep job re-fired every 5 minutes and failed again -- an infinite loop, and the student
        // watching the summary screen waited forever for a result that would never arrive.
        PracticeAttemptEvaluationFailedEventDto failed = jsonMapper.treeToValue(payload, PracticeAttemptEvaluationFailedEventDto.class);
        practiceItemResponseRepository.markGradingFailed(
            UUID.fromString(failed.practiceResponseId())
        );
        log.warn(
            "Practice grading failed practiceResponseId={} error={}",
            failed.practiceResponseId(),
            failed.payload() == null ? null : failed.payload().error()
        );
    }

    @DltHandler
    public void dltHandler(ConsumerRecord<String, Object> record) {
        log.error("Practice evaluation DLT message at topic {}: {}", record.topic(), record.value());
    }
}
