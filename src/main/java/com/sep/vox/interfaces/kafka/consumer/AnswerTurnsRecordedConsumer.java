package com.sep.vox.interfaces.kafka.consumer;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
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

import com.sep.vox.application.port.input.command.RecordAnswerTurnCommand;
import com.sep.vox.application.port.input.usecase.examitemresponse.RecordAnswerTerminationReasonUseCase;
import com.sep.vox.application.port.input.usecase.examitemresponse.RecordAnswerTurnUseCase;
import com.sep.vox.interfaces.kafka.dto.AnswerTurnsRecordedEventDto;

import tools.jackson.databind.json.JsonMapper;

@Component
public class AnswerTurnsRecordedConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnswerTurnsRecordedConsumer.class);

    private final RecordAnswerTurnUseCase recordAnswerTurnUseCase;
    private final RecordAnswerTerminationReasonUseCase recordAnswerTerminationReasonUseCase;
    private final JsonMapper jsonMapper;

    public AnswerTurnsRecordedConsumer(
            RecordAnswerTurnUseCase recordAnswerTurnUseCase,
            RecordAnswerTerminationReasonUseCase recordAnswerTerminationReasonUseCase,
            JsonMapper jsonMapper) {
        this.recordAnswerTurnUseCase = recordAnswerTurnUseCase;
        this.recordAnswerTerminationReasonUseCase = recordAnswerTerminationReasonUseCase;
        this.jsonMapper = jsonMapper;
    }

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
            var payload = jsonMapper.valueToTree(record.value());
            var event = jsonMapper.treeToValue(payload, AnswerTurnsRecordedEventDto.class);

            LOGGER.info("Receive answer turns event topic={} partition={} offset={} answerKey={}",
                record.topic(), record.partition(), record.offset(), record.key());

            if (event == null || event.payload() == null || event.payload().turns() == null) {
                ack.acknowledge();
                return;
            }
            if (event.eventType() != null && !"AnswerTurnsRecorded".equals(event.eventType())) {
                ack.acknowledge();
                return;
            }

            for (var turn : event.payload().turns()) {
                var answerId = parseUuid(turn.answerId() != null ? turn.answerId() : event.answerId(), "answerId");
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
            if (event.payload().reason() != null && !event.payload().reason().isBlank()) {
                recordAnswerTerminationReasonUseCase.execute(new RecordAnswerTerminationReasonUseCase.Command(
                    parseUuid(event.answerId(), "answerId"),
                    event.payload().reason().trim()
                ));
            }

            ack.acknowledge();
        } catch (Exception e) {
            LOGGER.error("Answer turns processed failed, no ack: {}", e.getMessage(), e);
            throw e instanceof RuntimeException runtimeException
                ? runtimeException
                : new IllegalStateException("Failed to process answer turns event", e);
        }
    }

    @DltHandler
    public void dltHandler(ConsumerRecord<String, Object> record) {
        LOGGER.error("Answer turns DLT message at topic {}: {}", record.topic(), record.value());
    }

    private UUID parseUuid(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " không được để trống");
        }

        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(fieldName + " không đúng định dạng UUID");
        }
    }

    private UUID parseNullableUuid(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(fieldName + " không đúng định dạng UUID");
        }
    }

    private OffsetDateTime parseAnsweredAt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("answeredAt không đúng định dạng ISO-8601");
        }
    }
}
