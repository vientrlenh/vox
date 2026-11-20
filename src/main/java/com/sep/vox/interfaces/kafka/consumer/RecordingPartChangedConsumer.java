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

import com.sep.vox.application.port.input.usecase.recording.RecordRecordingPartChangedUseCase;
import com.sep.vox.interfaces.kafka.dto.RecordingPartChangedEventDto;
import com.sep.vox.interfaces.kafka.mapper.RecordingPartChangedCommandMapper;

import tools.jackson.databind.json.JsonMapper;

@Component
public class RecordingPartChangedConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordingPartChangedConsumer.class);

    private final RecordRecordingPartChangedUseCase recordRecordingPartChangedUseCase;
    private final JsonMapper jsonMapper;

    public RecordingPartChangedConsumer(
            RecordRecordingPartChangedUseCase recordRecordingPartChangedUseCase,
            JsonMapper jsonMapper) {
        this.recordRecordingPartChangedUseCase = recordRecordingPartChangedUseCase;
        this.jsonMapper = jsonMapper;
    }

    @RetryableTopic(
        attempts = "4",
        backOff = @BackOff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
        autoCreateTopics = "true",
        dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(
        topics = "${app.external-event.kafka.consumer-groups.recording.topic.recording-part-changed}",
        groupId = "${app.external-event.kafka.consumer-groups.recording.group-id}"
    )
    public void consume(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        LOGGER.info(
            "Received recording part changed event partition={} offset={} key={}",
            record.partition(), record.offset(), record.key()
        );
        try {
            var payload = jsonMapper.valueToTree(record.value());
            var dto = jsonMapper.treeToValue(payload, RecordingPartChangedEventDto.class);
            recordRecordingPartChangedUseCase.execute(RecordingPartChangedCommandMapper.toCommand(dto));

            LOGGER.info(
                "Recorded recording part changed: streamId={} sessionId={} streamType={} status={}",
                dto.streamId(), dto.sessionId(), dto.streamType(), dto.status()
            );
            ack.acknowledge();
        } catch (Exception ex) {
            LOGGER.error("Recording part changed event processing failed, no ack: {}", ex.getMessage(), ex);
            throw (RuntimeException) ex;
        }
    }

    @DltHandler
    public void dltHandler(ConsumerRecord<String, Object> record) {
        LOGGER.error("Recording part changed DLT message at topic {}: {}", record.topic(), record.value());
    }
}
