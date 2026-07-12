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

import com.sep.vox.application.port.input.usecase.question.QuestionAssetAnalysisRequestPublisher;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.interfaces.kafka.dto.QuestionAssetAnalysisCompletedEventDto;

import tools.jackson.databind.json.JsonMapper;

@Component
public class QuestionAssetAnalysisCompletedConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionAssetAnalysisCompletedConsumer.class);

    private final QuestionAssetRepository questionAssetRepository;
    private final JsonMapper jsonMapper;

    public QuestionAssetAnalysisCompletedConsumer(
            QuestionAssetRepository questionAssetRepository,
            JsonMapper jsonMapper) {
        this.questionAssetRepository = questionAssetRepository;
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
            if (event == null || event.payload() == null || event.assetId() == null || event.assetId().isBlank()) {
                ack.acknowledge();
                return;
            }
            if (event.eventType() != null && !"QuestionAssetAnalysisCompleted".equals(event.eventType())) {
                ack.acknowledge();
                return;
            }

            var asset = questionAssetRepository.findById(UUID.fromString(event.assetId().trim())).orElse(null);
            if (asset == null) {
                ack.acknowledge();
                return;
            }

            // Re-read the asset fresh above (not a stale copy from publish time) and only write
            // into a field that is STILL blank right now -- a person may have typed a manual
            // value while this analysis was in flight, and that manual value always wins.
            var changed = false;
            if (QuestionAssetAnalysisRequestPublisher.supportsAiTranscript(asset.getType())
                    && event.payload().transcript() != null
                    && isBlank(asset.getTranscript())) {
                asset.setTranscript(event.payload().transcript());
                changed = true;
            }
            if (QuestionAssetAnalysisRequestPublisher.supportsDescription(asset.getType())
                    && event.payload().description() != null
                    && isBlank(asset.getDescription())) {
                asset.setDescription(event.payload().description());
                changed = true;
            }

            if (changed) {
                questionAssetRepository.save(asset);
            }

            ack.acknowledge();
        } catch (Exception ex) {
            LOGGER.error("Question asset analysis event processed failed, no ack: {}", ex.getMessage(), ex);
            throw ex instanceof RuntimeException runtimeException
                ? runtimeException
                : new IllegalStateException("Failed to process question asset analysis event", ex);
        }
    }

    @DltHandler
    public void dltHandler(ConsumerRecord<String, Object> record) {
        LOGGER.error("Question asset analysis DLT message at topic {}: {}", record.topic(), record.value());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
