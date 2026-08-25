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

import com.sep.vox.application.port.input.usecase.question.RecordQuestionAssetAnalysisResultUseCase;
import com.sep.vox.interfaces.kafka.dto.QuestionAssetAnalysisCompletedEventDto;

import tools.jackson.databind.json.JsonMapper;

@Component
public class QuestionAssetAnalysisCompletedConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionAssetAnalysisCompletedConsumer.class);

    private final RecordQuestionAssetAnalysisResultUseCase recordQuestionAssetAnalysisResultUseCase;
    private final JsonMapper jsonMapper;

    public QuestionAssetAnalysisCompletedConsumer(
            RecordQuestionAssetAnalysisResultUseCase recordQuestionAssetAnalysisResultUseCase,
            JsonMapper jsonMapper) {
        this.recordQuestionAssetAnalysisResultUseCase = recordQuestionAssetAnalysisResultUseCase;
        this.jsonMapper = jsonMapper;
    }

    @RetryableTopic(
        attempts = "4",
        backOff = @BackOff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
        autoCreateTopics = "true",
        dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(
        topics = "${app.external-event.kafka.consumer-groups.question-asset-analysis.topic.question-asset-analysis-completed}",
        groupId = "${app.external-event.kafka.consumer-groups.question-asset-analysis.group-id}"
    )
    public void consume(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        try {
            var payload = jsonMapper.valueToTree(record.value());
            var event = jsonMapper.treeToValue(payload, QuestionAssetAnalysisCompletedEventDto.class);

            LOGGER.info("Receive question asset analysis event topic={} partition={} offset={} assetId={}",
                record.topic(), record.partition(), record.offset(), event == null ? null : event.assetId());

            if (event == null || event.assetId() == null || event.assetId().isBlank()) {
                ack.acknowledge();
                return;
            }
            if (event.eventType() != null && !"QuestionAssetAnalysisCompleted".equals(event.eventType())) {
                ack.acknowledge();
                return;
            }

            var payloadDto = event.payload();
            recordQuestionAssetAnalysisResultUseCase.execute(new RecordQuestionAssetAnalysisResultUseCase.Command(
                UUID.fromString(event.assetId().trim()),
                payloadDto == null ? null : payloadDto.transcript(),
                payloadDto == null ? null : payloadDto.description()
            ));

            ack.acknowledge();
        } catch (Exception e) {
            LOGGER.error("Question asset analysis event processed failed, no ack: {}", e.getMessage(), e);
            throw e instanceof RuntimeException runtimeException
                ? runtimeException
                : new IllegalStateException("Failed to process question asset analysis event", e);
        }
    }

    @DltHandler
    public void dltHandler(ConsumerRecord<String, Object> record) {
        LOGGER.error("Question asset analysis DLT message at topic {}: {}", record.topic(), record.value());
    }
}
