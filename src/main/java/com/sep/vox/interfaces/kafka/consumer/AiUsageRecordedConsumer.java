package com.sep.vox.interfaces.kafka.consumer;

import java.time.Instant;
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

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.command.RecordAiUsageCommand;
import com.sep.vox.application.port.input.usecase.subscription.RecordAiUsageUseCase;
import com.sep.vox.domain.model.metering.AiUsageType;
import com.sep.vox.interfaces.kafka.dto.AiUsageRecordedEventDto;

import tools.jackson.databind.json.JsonMapper;

@Component
public class AiUsageRecordedConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiUsageRecordedConsumer.class);

    private final RecordAiUsageUseCase recordAiUsageUseCase;
    private final JsonMapper jsonMapper;

    public AiUsageRecordedConsumer(RecordAiUsageUseCase recordAiUsageUseCase, JsonMapper jsonMapper) {
        this.recordAiUsageUseCase = recordAiUsageUseCase;
        this.jsonMapper = jsonMapper;
    }

    @RetryableTopic(
        attempts = "4",
        backOff = @BackOff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
        autoCreateTopics = "true",
        dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(
        topics = "${app.external-event.kafka.consumer-groups.ai-usage.topic.ai-usage-recorded}",
        groupId = "${app.external-event.kafka.consumer-groups.ai-usage.group-id}"
    )
    public void consume(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        try {
            var payload = jsonMapper.valueToTree(record.value());
            var event = jsonMapper.treeToValue(payload, AiUsageRecordedEventDto.class);

            LOGGER.info("Receive AI usage event topic={} partition={} offset={} turnId={}",
                record.topic(), record.partition(), record.offset(), event == null ? null : event.turnId());

            if (event == null || event.usageEvents() == null || event.usageEvents().isEmpty()) {
                ack.acknowledge();
                return;
            }
            if (event.eventType() != null && !"AiUsageRecorded".equals(event.eventType())) {
                ack.acknowledge();
                return;
            }

            var examSessionId = parseUuid(event.examSessionId(), "examSessionId");
            var turnId = parseUuid(event.turnId(), "turnId");

            for (var item : event.usageEvents()) {
                var usage = item.usage();
                recordAiUsageUseCase.execute(new RecordAiUsageCommand(
                    examSessionId,
                    turnId,
                    parseUuid(item.usageEventId(), "usageEventId"),
                    parseUsageType(item.type()),
                    item.provider(),
                    item.model(),
                    usage == null ? null : usage.inputTokens(),
                    usage == null ? null : usage.outputTokens(),
                    usage == null ? null : usage.cacheCreationInputTokens(),
                    usage == null ? null : usage.cacheReadInputTokens(),
                    item.durationMs(),
                    item.unitPrice() == null ? "{}" : jsonMapper.writeValueAsString(item.unitPrice()),
                    item.costUsd(),
                    parseOccurredAt(item.occurredAt())
                ));
            }

            ack.acknowledge();
        } catch (Exception e) {
            LOGGER.error("AI usage event processing failed, no ack: {}", e.getMessage(), e);
            throw e instanceof RuntimeException runtimeException
                ? runtimeException
                : new IllegalStateException("Failed to process AI usage event", e);
        }
    }

    @DltHandler
    public void dltHandler(ConsumerRecord<String, Object> record) {
        LOGGER.error("AI usage DLT message at topic {}: {}", record.topic(), record.value());
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

    private AiUsageType parseUsageType(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("type không được để trống");
        }

        try {
            return AiUsageType.valueOf(StringNormalization.normalizeCode(value));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("type không hợp lệ: " + value);
        }
    }

    private Instant parseOccurredAt(String value) {
        if (value == null || value.isBlank()) {
            return Instant.now();
        }

        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("occurredAt không đúng định dạng ISO-8601");
        }
    }
}