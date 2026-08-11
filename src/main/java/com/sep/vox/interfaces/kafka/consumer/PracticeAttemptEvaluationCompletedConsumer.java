package com.sep.vox.interfaces.kafka.consumer;

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

import com.sep.vox.application.port.input.usecase.practiceevaluation.RecordPracticeAttemptEvaluationUseCase;
import com.sep.vox.interfaces.kafka.dto.PracticeAttemptEvaluationCompletedEventDto;
import com.sep.vox.interfaces.kafka.mapper.RecordPracticeAttemptEvaluationCommandMapper;

import tools.jackson.databind.json.JsonMapper;

@Component
public class PracticeAttemptEvaluationCompletedConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        PracticeAttemptEvaluationCompletedConsumer.class
    );

    private final RecordPracticeAttemptEvaluationUseCase recordPracticeAttemptEvaluationUseCase;
    private final JsonMapper jsonMapper;

    public PracticeAttemptEvaluationCompletedConsumer(
            RecordPracticeAttemptEvaluationUseCase recordPracticeAttemptEvaluationUseCase,
            JsonMapper jsonMapper) {
        this.recordPracticeAttemptEvaluationUseCase = recordPracticeAttemptEvaluationUseCase;
        this.jsonMapper = jsonMapper;
    }

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
        LOGGER.info(
            "Received practice evaluation event partition={} offset={} key={}",
            record.partition(), record.offset(), record.key()
        );
        try {
            var payload = jsonMapper.valueToTree(record.value());
            var eventType = payload.path("eventType").asString();

            if ("PracticeAttemptEvaluationCompleted".equals(eventType)) {
                var dto = jsonMapper.treeToValue(payload, PracticeAttemptEvaluationCompletedEventDto.class);
                recordPracticeAttemptEvaluationUseCase.execute(
                    RecordPracticeAttemptEvaluationCommandMapper.toCommand(dto)
                );
                LOGGER.info("Recorded practice evaluation for practiceResponseId={}", dto.practiceResponseId());
            } else {
                LOGGER.info("Skip unknown practice evaluation event type={}", eventType);
            }

            ack.acknowledge();
        } catch (Exception ex) {
            LOGGER.error("Practice evaluation event processed failed, no ack: {}", ex.getMessage(), ex);
            throw ex instanceof RuntimeException runtimeException
                ? runtimeException
                : new IllegalStateException("Failed to process practice evaluation event", ex);
        }
    }

    @DltHandler
    public void dltHandler(ConsumerRecord<String, Object> record) {
        LOGGER.error("Practice evaluation DLT message at topic {}: {}", record.topic(), record.value());
    }
}
