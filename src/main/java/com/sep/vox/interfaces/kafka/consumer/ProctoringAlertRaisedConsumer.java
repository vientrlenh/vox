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

import tools.jackson.databind.json.JsonMapper;

/**
 * Đầu đọc của topic {@code exam.alert.raised}.
 *
 * <p>vox-streaming đã phát topic này từ trước mà không ai tiêu thụ, nên mọi cảnh báo giám sát chỉ
 * tồn tại trong bộ nhớ tab trình duyệt của giám thị đang mở. Consumer này là chỗ chúng trở thành bản
 * ghi: phát lại được cho giám thị vào ca muộn, và tra lại được khi chấm bài sau thi.
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
            var payload = jsonMapper.valueToTree(record.value());
            var dto = jsonMapper.treeToValue(payload, AlertRaisedEventDto.class);
            var stored = recordProctoringAlertUseCase.execute(ProctoringAlertCommandMapper.toCommand(dto));

            // Chỉ log ở mức INFO khi thật sự có dòng mới. Cảnh báo lặp lại là chuyện bình thường của
            // Kafka, và ghi log mỗi lần gửi lại sẽ biến log giám sát thành nhiễu đúng lúc cần đọc nó
            // nhất -- khi đang có sự cố.
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
