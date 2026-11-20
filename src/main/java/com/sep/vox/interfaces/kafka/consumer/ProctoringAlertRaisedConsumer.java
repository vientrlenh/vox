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

import com.sep.vox.application.port.input.usecase.proctoring.RecordProctoringAlertUseCase;
import com.sep.vox.interfaces.kafka.dto.AlertRaisedEventDto;
import com.sep.vox.interfaces.kafka.mapper.ProctoringAlertCommandMapper;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The reader of the topic {@code assessment.alert.raised}.
 *
 * <p>vox-streaming has published this topic for a long time without anyone consuming it, so every proctoring alert only
 * existed in the memory of the open proctor's browser tab. This consumer is where they become a
 * record: replayable for a proctor who joins a session late, and searchable when grading after the assessment.
 */
@Component
public class ProctoringAlertRaisedConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProctoringAlertRaisedConsumer.class);

    private final RecordProctoringAlertUseCase recordProctoringAlertUseCase;
    private final JsonMapper jsonMapper;

    public ProctoringAlertRaisedConsumer(
            RecordProctoringAlertUseCase recordProctoringAlertUseCase,
            JsonMapper jsonMapper) {
        this.recordProctoringAlertUseCase = recordProctoringAlertUseCase;
        this.jsonMapper = jsonMapper;
    }

    @RetryableTopic(
        attempts = "4",
        backOff = @BackOff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
        autoCreateTopics = "true",
        dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(
        topics = "${app.external-event.kafka.consumer-groups.proctoring-alert.topic.alert-raised}",
        groupId = "${app.external-event.kafka.consumer-groups.proctoring-alert.group-id}"
    )
    public void consume(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        try {
            JsonNode payload = jsonMapper.valueToTree(record.value());
            AlertRaisedEventDto dto = jsonMapper.treeToValue(payload, AlertRaisedEventDto.class);
            Boolean stored = recordProctoringAlertUseCase.execute(ProctoringAlertCommandMapper.toCommand(dto));

            // Log at INFO only when a new row was actually stored. Repeated alerts are normal with Kafka, and
            // logging every redelivery would turn the monitoring log into noise exactly when it needs to be
            // read the most -- during an incident.
            if (Boolean.TRUE.equals(stored)) {
                LOGGER.info(
                    "Đã lưu cảnh báo giám sát: eventId={} sessionId={} alertType={} capturedAt={}",
                    dto.eventId(), dto.sessionId(), dto.alertType(), dto.capturedAt()
                );
            } else {
                LOGGER.debug("Bỏ qua cảnh báo giám sát trùng hoặc không dùng được: eventId={}", dto.eventId());
            }
            ack.acknowledge();
        } catch (Exception ex) {
            LOGGER.error(
                "Xử lý cảnh báo giám sát thất bại, không ack: partition={} offset={} key={} lỗi={}",
                record.partition(), record.offset(), record.key(), ex.getMessage(), ex
            );
            throw (RuntimeException) ex;
        }
    }

    @DltHandler
    public void dltHandler(ConsumerRecord<String, Object> record) {
        LOGGER.error("Cảnh báo giám sát rơi vào DLT ở topic {}: {}", record.topic(), record.value());
    }
}
