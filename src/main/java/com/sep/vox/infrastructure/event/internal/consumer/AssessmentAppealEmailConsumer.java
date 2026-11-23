package com.sep.vox.infrastructure.event.internal.consumer;

import com.sep.vox.domain.model.user.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.event.AssessmentAppealApprovedPayloadV1;
import com.sep.vox.application.event.AssessmentAppealPublishedPayloadV1;
import com.sep.vox.application.event.AssessmentAppealRejectedPayloadV1;
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
@RequiredArgsConstructor
@Slf4j
public class AssessmentAppealEmailConsumer {

    private static final String CONSUMER_GROUP = "assessment-appeal-email";

    private static final String SUBJECT_PUBLISHED = "Kết quả phúc khảo đã được công bố";
    private static final String SUBJECT_REJECTED = "Đơn phúc khảo không được chấp nhận";
    private static final String SUBJECT_APPROVED = "Đơn phúc khảo của bạn đã được duyệt";

    /** {@code withZone} is mandatory: without it the email prints the container's UTC time. */
    private static final DateTimeFormatter DEADLINE_FORMAT =
        DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(DateMapper.DEFAULT_INPUT_ZONE);

    private final UserRepository userRepository;
    private final MailSendingPort mailSendingPort;
    private final MailTemplatePort mailTemplatePort;
    private final ProcessedEventRepository processedEventRepository;
    private final JsonMapper jsonMapper;

    @RetryableTopic(
        attempts = "4",
        backOff = @BackOff(delay = 2000, multiplier = 2.0, maxDelay = 30000),
        autoCreateTopics = "true",
        dltStrategy = DltStrategy.FAIL_ON_ERROR,
        kafkaTemplate = "outboxKafkaTemplate"
    )
    @KafkaListener(
        topics = "${app.internal-event.kafka.consumer-groups.email.topic.assessment-appeal}",
        groupId = "${app.internal-event.kafka.consumer-groups.email.group-id}",
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        UUID eventId = KafkaEventHeaders.readEventId(record);

        log.info("Receive event assessment-appeal: eventId={}, partition={}, offset={}",
            eventId, record.partition(), record.offset());

        if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
            log.info("Skip processed event: eventId={}, consumer={}", eventId, CONSUMER_GROUP);
            ack.acknowledge();
            return;
        }

        String eventType = KafkaEventHeaders.readEventType(record);
        Mail mail = renderMail(eventType, eventId, record.value());

        // If the student has been deleted, resending any number of times will never find a recipient: mark
        // it processed and skip, do not let the message run the whole retry cycle and end up in the DLT.
        if (mail == null) {
            markProcessed(eventId);
            log.warn("Skip mail, recipient not found: eventId={}, eventType={}", eventId, eventType);
            ack.acknowledge();
            return;
        }

        try {
            mailSendingPort.sendHtml(mail.to(), mail.subject(), mail.html());
        } catch (Exception e) {
            log.error("Send mail error: eventId={}, to={}", eventId, mail.to(), e);
            throw new IllegalStateException("Gửi mail phúc khảo thất bại: eventId=" + eventId, e);
        }

        markProcessed(eventId);

        log.info("Assessment appeal mail sent: eventId={}, eventType={}, to={}", eventId, eventType, mail.to());
        ack.acknowledge();
    }

    /** @return {@code null} when no recipient can be found */
    private Mail renderMail(String eventType, UUID eventId, String value) {
        return switch (eventType) {
            case EventTypeConstant.ASSESSMENT_APPEAL_PUBLISHED -> {
                AssessmentAppealPublishedPayloadV1 payload = parse(value, AssessmentAppealPublishedPayloadV1.class, eventType, eventId);
                yield mail(payload.studentId(), SUBJECT_PUBLISHED, () -> mailTemplatePort.renderAppealPublishedEmail(
                    payload.assessmentName(), formatScore(payload.scoreBefore()), formatScore(payload.scoreAfter())));
            }
            case EventTypeConstant.ASSESSMENT_APPEAL_REJECTED -> {
                AssessmentAppealRejectedPayloadV1 payload = parse(value, AssessmentAppealRejectedPayloadV1.class, eventType, eventId);
                yield mail(payload.studentId(), SUBJECT_REJECTED,
                    () -> mailTemplatePort.renderAppealRejectedEmail(payload.assessmentName(), payload.reason()));
            }
            case EventTypeConstant.ASSESSMENT_APPEAL_APPROVED -> {
                AssessmentAppealApprovedPayloadV1 payload = parse(value, AssessmentAppealApprovedPayloadV1.class, eventType, eventId);
                yield mail(payload.studentId(), SUBJECT_APPROVED, () -> mailTemplatePort.renderAppealApprovedEmail(
                    payload.assessmentName(), formatDeadline(payload.deadline())));
            }
            default -> throw new IllegalStateException(
                "eventType không hợp lệ: " + eventType + ", eventId=" + eventId);
        };
    }

    /** Defer rendering until a recipient is certain, so we do not build HTML just to throw it away. */
    private Mail mail(UUID recipientId, String subject, Supplier<String> html) {
        if (recipientId == null) {
            return null;
        }
        User recipient = userRepository.findById(recipientId).orElse(null);
        if (recipient == null) {
            return null;
        }
        return new Mail(recipient.getEmail().value(), subject, html.get());
    }

    private <T> T parse(String value, Class<T> type, String eventType, UUID eventId) {
        try {
            return jsonMapper.readValue(value, type);
        } catch (Exception e) {
            log.error("Invalid payload: eventId={}, eventType={}, value={}", eventId, eventType, value, e);
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
        UUID eventId = KafkaEventHeaders.readEventIdOrNull(record);
        log.error("assessment-appeal event sent to DLT: eventId={}, topic={}, partition={}, offset={}, payload={}",
            eventId, record.topic(), record.partition(), record.offset(), record.value());
    }

    private void markProcessed(UUID eventId) {
        try {
            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(eventId)
                    .consumerGroup(CONSUMER_GROUP)
                    .processedAt(Instant.now())
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.warn("This event was already marked by another instance: eventId={}", eventId);
        }
    }

    private record Mail(String to, String subject, String html) {

    }
}
