package com.sep.vox.infrastructure.event.internal.consumer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.function.Supplier;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.event.ExamAppealApprovedPayloadV1;
import com.sep.vox.application.event.ExamAppealPublishedPayloadV1;
import com.sep.vox.application.event.ExamAppealRejectedPayloadV1;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.outbox.ProcessedEvent;
import com.sep.vox.domain.repository.ProcessedEventRepository;
import com.sep.vox.domain.repository.UserRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * Mail báo học sinh mọi mốc trong vòng đời một đơn phúc khảo.
 *
 * <p>Ba eventType dùng chung một topic vì cùng người nhận và cùng khuôn xử lý: giải
 * {@code studentId} thành email rồi render template. Phân nhánh bằng header
 * {@code eventType} thay vì nhét cờ phân loại vào payload, nhờ vậy mỗi loại vẫn giữ
 * được record riêng không có field thừa.
 */
@Component
public class ExamAppealEmailConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExamAppealEmailConsumer.class);

    private static final String CONSUMER_GROUP = "exam-appeal-email";

    private static final String SUBJECT_PUBLISHED = "Kết quả phúc khảo đã được công bố";
    private static final String SUBJECT_REJECTED = "Đơn phúc khảo không được chấp nhận";
    private static final String SUBJECT_APPROVED = "Đơn phúc khảo của bạn đã được duyệt";

    /** {@code withZone} là phần bắt buộc: thiếu nó thì mail ghi giờ UTC của container. */
    private static final DateTimeFormatter DEADLINE_FORMAT =
        DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(DateMapper.DEFAULT_INPUT_ZONE);

    private final UserRepository userRepository;
    private final MailSendingPort mailSendingPort;
    private final MailTemplatePort mailTemplatePort;
    private final ProcessedEventRepository processedEventRepository;
    private final JsonMapper jsonMapper;

    public ExamAppealEmailConsumer(
            UserRepository userRepository,
            MailSendingPort mailSendingPort,
            MailTemplatePort mailTemplatePort,
            ProcessedEventRepository processedEventRepository,
            JsonMapper jsonMapper) {
        this.userRepository = userRepository;
        this.mailSendingPort = mailSendingPort;
        this.mailTemplatePort = mailTemplatePort;
        this.processedEventRepository = processedEventRepository;
        this.jsonMapper = jsonMapper;
    }

    @RetryableTopic(
        attempts = "4",
        backOff = @BackOff(delay = 2000, multiplier = 2.0, maxDelay = 30000),
        autoCreateTopics = "true",
        dltStrategy = DltStrategy.FAIL_ON_ERROR,
        kafkaTemplate = "outboxKafkaTemplate"
    )
    @KafkaListener(
        topics = "${app.internal-event.kafka.consumer-groups.email.topic.exam-appeal}",
        groupId = "${app.internal-event.kafka.consumer-groups.email.group-id}",
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        var eventId = KafkaEventHeaders.readEventId(record);

        LOGGER.info("Receive event exam-appeal: eventId={}, partition={}, offset={}",
            eventId, record.partition(), record.offset());

        if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
            LOGGER.info("Skip processed event: eventId={}, consumer={}", eventId, CONSUMER_GROUP);
            ack.acknowledge();
            return;
        }

        var eventType = KafkaEventHeaders.readEventType(record);
        var mail = renderMail(eventType, eventId, record.value());

        // Học sinh đã bị xoá thì gửi lại bao nhiêu lần cũng không có người nhận: đánh dấu
        // đã xử lý rồi bỏ qua, đừng để message chạy hết vòng retry rồi rơi vào DLT.
        if (mail == null) {
            markProcessed(eventId);
            LOGGER.warn("Skip mail, recipient not found: eventId={}, eventType={}", eventId, eventType);
            ack.acknowledge();
            return;
        }

        try {
            mailSendingPort.sendHtml(mail.to(), mail.subject(), mail.html());
        } catch (Exception e) {
            LOGGER.error("Send mail error: eventId={}, to={}", eventId, mail.to(), e);
            throw new IllegalStateException("Gửi mail phúc khảo thất bại: eventId=" + eventId, e);
        }

        markProcessed(eventId);

        LOGGER.info("Exam appeal mail sent: eventId={}, eventType={}, to={}", eventId, eventType, mail.to());
        ack.acknowledge();
    }

    /** @return {@code null} khi không tìm được người nhận */
    private Mail renderMail(String eventType, UUID eventId, String value) {
        return switch (eventType) {
            case EventTypeConstant.EXAM_APPEAL_PUBLISHED -> {
                var payload = parse(value, ExamAppealPublishedPayloadV1.class, eventType, eventId);
                yield mail(payload.studentId(), SUBJECT_PUBLISHED, () -> mailTemplatePort.renderAppealPublishedEmail(
                    payload.examName(), formatScore(payload.scoreBefore()), formatScore(payload.scoreAfter())));
            }
            case EventTypeConstant.EXAM_APPEAL_REJECTED -> {
                var payload = parse(value, ExamAppealRejectedPayloadV1.class, eventType, eventId);
                yield mail(payload.studentId(), SUBJECT_REJECTED,
                    () -> mailTemplatePort.renderAppealRejectedEmail(payload.examName(), payload.reason()));
            }
            case EventTypeConstant.EXAM_APPEAL_APPROVED -> {
                var payload = parse(value, ExamAppealApprovedPayloadV1.class, eventType, eventId);
                yield mail(payload.studentId(), SUBJECT_APPROVED, () -> mailTemplatePort.renderAppealApprovedEmail(
                    payload.examName(), formatDeadline(payload.deadline())));
            }
            default -> throw new IllegalStateException(
                "eventType không hợp lệ: " + eventType + ", eventId=" + eventId);
        };
    }

    /** Hoãn render tới khi đã chắc chắn có người nhận, khỏi dựng HTML để rồi vứt đi. */
    private Mail mail(UUID recipientId, String subject, Supplier<String> html) {
        if (recipientId == null) {
            return null;
        }
        var recipient = userRepository.findById(recipientId).orElse(null);
        if (recipient == null) {
            return null;
        }
        return new Mail(recipient.getEmail().value(), subject, html.get());
    }

    private <T> T parse(String value, Class<T> type, String eventType, UUID eventId) {
        try {
            return jsonMapper.readValue(value, type);
        } catch (Exception e) {
            LOGGER.error("Invalid payload: eventId={}, eventType={}, value={}", eventId, eventType, value, e);
            throw new IllegalStateException(
                "Payload " + eventType + " không hợp lệ, eventId=" + eventId, e);
        }
    }

    private String formatScore(BigDecimal score) {
        return score == null ? "-" : score.toPlainString();
    }

    private String formatDeadline(Instant deadline) {
        return deadline == null ? "chưa đặt" : DEADLINE_FORMAT.format(deadline);
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, String> record) {
        var eventId = KafkaEventHeaders.readEventIdOrNull(record);
        LOGGER.error("exam-appeal event sent to DLT: eventId={}, topic={}, partition={}, offset={}, payload={}",
            eventId, record.topic(), record.partition(), record.offset(), record.value());
    }

    private void markProcessed(UUID eventId) {
        try {
            processedEventRepository.save(new ProcessedEvent(eventId, CONSUMER_GROUP, Instant.now()));
        } catch (DataIntegrityViolationException e) {
            LOGGER.warn("This event was already marked by another instance: eventId={}", eventId);
        }
    }

    private record Mail(String to, String subject, String html) {

    }
}
