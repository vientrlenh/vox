package com.sep.vox.interfaces.kafka.consumer;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

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

import com.sep.vox.application.port.input.command.assessmentitemresponse.RecordAnswerTurnCommand;
import com.sep.vox.application.port.input.usecase.assessmentitemresponse.RecordAnswerTerminationReasonUseCase;
import com.sep.vox.application.port.input.usecase.assessmentitemresponse.RecordAnswerTurnUseCase;
import com.sep.vox.interfaces.kafka.dto.AnswerTurnsRecordedEventDto;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnswerTurnsRecordedConsumer {

    private final RecordAnswerTurnUseCase recordAnswerTurnUseCase;
    private final RecordAnswerTerminationReasonUseCase recordAnswerTerminationReasonUseCase;
    private final JsonMapper jsonMapper;

    @RetryableTopic(
        attempts = "4",
        backOff = @BackOff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
        autoCreateTopics = "true",
        dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(
        topics = "${app.external-event.kafka.consumer-groups.answer-turns.topic.answer-turns-recorded}",
        groupId = "${app.external-event.kafka.consumer-groups.answer-turns.group-id}"
    )
    public void consume(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        try {
            JsonNode payload = jsonMapper.valueToTree(record.value());
            AnswerTurnsRecordedEventDto event = jsonMapper.treeToValue(payload, AnswerTurnsRecordedEventDto.class);

            log.info("Receive answer turns event topic={} partition={} offset={} answerKey={}",
                record.topic(), record.partition(), record.offset(), record.key());

            if (isIgnorable(event)) {
                ack.acknowledge();
                return;
            }

            recordTurns(event);
            recordTerminationReason(event);

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Answer turns processed failed, no ack: {}", e.getMessage(), e);
            throw e instanceof RuntimeException runtimeException
                ? runtimeException
                : new IllegalStateException("Failed to process answer turns event", e);
        }
    }

    private boolean isIgnorable(AnswerTurnsRecordedEventDto event) {
        return event == null || event.payload() == null || event.payload().turns() == null
            || (event.eventType() != null && !"AnswerTurnsRecorded".equals(event.eventType()));
    }

    private void recordTurns(AnswerTurnsRecordedEventDto event) {
        for (AnswerTurnsRecordedEventDto.AnswerTurnPayloadDto turn : event.payload().turns()) {
            UUID answerId = parseUuid(turn.answerId() != null ? turn.answerId() : event.answerId(), "answerId");
            recordAnswerTurnUseCase.execute(new RecordAnswerTurnCommand(
                answerId,
                parseUuid(turn.sessionId(), "sessionId"),
                parseNullableUuid(turn.paperItemId(), "paperItemId"),
                turn.turnOrder() == null ? 0 : turn.turnOrder(),
                turn.turnType(),
                turn.promptText(),
                turn.audioUrl(),
                turn.transcript(),
                turn.durationSeconds(),
                turn.wordCount(),
                parseAnsweredAt(turn.answeredAt())
            ));
        }
    }

    private void recordTerminationReason(AnswerTurnsRecordedEventDto event) {
        if (event.payload().reason() != null && !event.payload().reason().isBlank()) {
            recordAnswerTerminationReasonUseCase.execute(new RecordAnswerTerminationReasonUseCase.Command(
                parseUuid(event.answerId(), "answerId"),
                event.payload().reason().trim()
            ));
        }
    }

    @DltHandler
    public void dltHandler(ConsumerRecord<String, Object> record) {
        log.error("Answer turns DLT message at topic {}: {}", record.topic(), record.value());
    }

    private UUID parseUuid(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(fieldName + " is not a valid UUID");
        }
    }

    private UUID parseNullableUuid(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(fieldName + " is not a valid UUID");
        }
    }

    private Instant parseAnsweredAt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("answeredAt is not in ISO-8601 format");
        }
    }
}
