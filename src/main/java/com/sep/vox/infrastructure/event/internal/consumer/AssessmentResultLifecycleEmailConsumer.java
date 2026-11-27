package com.sep.vox.infrastructure.event.internal.consumer;

import com.sep.vox.domain.model.user.User;
import java.math.BigDecimal;
import java.time.Instant;
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

import com.sep.vox.application.event.AssessmentResultInvalidClearedPayloadV1;
import com.sep.vox.application.event.AssessmentResultInvalidatedPayloadV1;
import com.sep.vox.application.event.AssessmentResultOutcomeDecidedPayloadV1;
import com.sep.vox.application.event.AssessmentResultRegradedPayloadV1;
import com.sep.vox.application.event.AssessmentResultReleasedPayloadV1;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.model.outbox.ProcessedEvent;
import com.sep.vox.domain.repository.ProcessedEventRepository;
import com.sep.vox.domain.repository.UserRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * Mail báo học sinh mọi mốc trong vòng đời điểm của họ.
 *
 * <p>Gom năm eventType vào một topic vì chúng dùng chung đúng một khuôn: giải
 * {@code studentId} thành email rồi render template. Tách thành năm consumer chỉ nhân
 * bản cùng một constructor năm lần.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AssessmentResultLifecycleEmailConsumer {

    private static final String CONSUMER_GROUP = "assessment-result-lifecycle-email";

    private static final String SUBJECT_RELEASED = "Điểm thi của bạn đã có";
    private static final String SUBJECT_REGRADED = "Điểm thi của bạn đã thay đổi";
    private static final String SUBJECT_INVALIDATED = "Bài thi của bạn đã bị vô hiệu";
    private static final String SUBJECT_INVALID_CLEARED = "Bài thi của bạn đã được khôi phục";
    private static final String SUBJECT_OUTCOME_DECIDED = "Kết quả cuối cùng của bạn";

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
        topics = "${app.internal-event.kafka.consumer-groups.email.topic.assessment-result-lifecycle}",
        groupId = "${app.internal-event.kafka.consumer-groups.email.group-id}",
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        UUID eventId = KafkaEventHeaders.readEventId(record);

        log.info("Receive event assessment-result-lifecycle: eventId={}, partition={}, offset={}",
            eventId, record.partition(), record.offset());

        if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
            log.info("Skip processed event: eventId={}, consumer={}", eventId, CONSUMER_GROUP);
            ack.acknowledge();
            return;
        }

        String eventType = KafkaEventHeaders.readEventType(record);
        Mail mail = renderMail(eventType, eventId, record.value());

        if (mail == null) {
            skipMissingRecipient(eventId, eventType, ack);
            return;
        }

        deliverMail(mail, eventId, eventType);
        ack.acknowledge();
    }

    /**
     * If the student has been deleted, resending any number of times will never find a recipient: mark
     * it processed and skip, do not let the message run the whole retry cycle and end up in the DLT.
     */
    private void skipMissingRecipient(UUID eventId, String eventType, Acknowledgment ack) {
        markProcessed(eventId);
        log.warn("Skip mail, recipient not found: eventId={}, eventType={}", eventId, eventType);
        ack.acknowledge();
    }

    private void deliverMail(Mail mail, UUID eventId, String eventType) {
        sendMail(mail, eventId);
        markProcessed(eventId);

        log.info("Assessment result lifecycle mail sent: eventId={}, eventType={}, to={}",
            eventId, eventType, mail.to());
    }

    private void sendMail(Mail mail, UUID eventId) {
        try {
            mailSendingPort.sendHtml(mail.to(), mail.subject(), mail.html());
        } catch (Exception e) {
            log.error("Send mail error: eventId={}, to={}", eventId, mail.to(), e);
            throw new IllegalStateException("Gửi mail vòng đời điểm thất bại: eventId=" + eventId, e);
        }
    }

    /** @return {@code null} when no recipient can be found */
    private Mail renderMail(String eventType, UUID eventId, String value) {
        return switch (eventType) {
            case EventTypeConstant.ASSESSMENT_RESULT_RELEASED -> renderReleasedMail(value, eventType, eventId);
            case EventTypeConstant.ASSESSMENT_RESULT_REGRADED -> renderRegradedMail(value, eventType, eventId);
            case EventTypeConstant.ASSESSMENT_RESULT_INVALIDATED -> renderInvalidatedMail(value, eventType, eventId);
            case EventTypeConstant.ASSESSMENT_RESULT_INVALID_CLEARED ->
                renderInvalidClearedMail(value, eventType, eventId);
            case EventTypeConstant.ASSESSMENT_RESULT_OUTCOME_DECIDED ->
                renderOutcomeDecidedMail(value, eventType, eventId);
            default -> throw new IllegalStateException(
                "eventType không hợp lệ: " + eventType + ", eventId=" + eventId);
        };
    }

    private Mail renderReleasedMail(String value, String eventType, UUID eventId) {
        AssessmentResultReleasedPayloadV1 payload = parse(value, AssessmentResultReleasedPayloadV1.class, eventType, eventId);
        return mail(payload.studentId(), SUBJECT_RELEASED, () -> mailTemplatePort.renderResultReleasedEmail(
            payload.assessmentName(), formatScore(payload.totalScore())));
    }

    private Mail renderRegradedMail(String value, String eventType, UUID eventId) {
        AssessmentResultRegradedPayloadV1 payload = parse(value, AssessmentResultRegradedPayloadV1.class, eventType, eventId);
        return mail(payload.studentId(), SUBJECT_REGRADED, () -> mailTemplatePort.renderResultRegradedEmail(
            payload.assessmentName(),
            roundLabel(payload.roundType()),
            formatScore(payload.scoreBefore()),
            formatScore(payload.scoreAfter())));
    }

    private Mail renderInvalidatedMail(String value, String eventType, UUID eventId) {
        AssessmentResultInvalidatedPayloadV1 payload = parse(value, AssessmentResultInvalidatedPayloadV1.class, eventType, eventId);
        return mail(payload.studentId(), SUBJECT_INVALIDATED,
            () -> mailTemplatePort.renderResultInvalidatedEmail(payload.assessmentName(), payload.reason()));
    }

    private Mail renderInvalidClearedMail(String value, String eventType, UUID eventId) {
        AssessmentResultInvalidClearedPayloadV1 payload = parse(value, AssessmentResultInvalidClearedPayloadV1.class, eventType, eventId);
        return mail(payload.studentId(), SUBJECT_INVALID_CLEARED,
            () -> mailTemplatePort.renderResultInvalidClearedEmail(payload.assessmentName(), payload.reason()));
    }

    private Mail renderOutcomeDecidedMail(String value, String eventType, UUID eventId) {
        AssessmentResultOutcomeDecidedPayloadV1 payload = parse(value, AssessmentResultOutcomeDecidedPayloadV1.class, eventType, eventId);
        return mail(payload.studentId(), SUBJECT_OUTCOME_DECIDED,
            () -> mailTemplatePort.renderResultOutcomeDecidedEmail(
                payload.assessmentName(),
                outcomeLabel(payload.outcome()),
                formatScore(payload.totalScore())));
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

    /**
     * Học sinh không cần biết tên kỹ thuật của vòng chấm.
     *
     * <p>Giá trị lạ trả về nhãn mặc định thay vì ném: message cũ còn nằm trong topic có
     * thể mang tên vòng đã bị đổi hoặc xoá, và một cái nhãn hiển thị không đáng để đẩy
     * cả thông báo điểm vào DLT.
     */
    private String roundLabel(String roundType) {
        if (roundType == null) {
            return "chấm lại";
        }
        GradingRoundType round;
        try {
            round = GradingRoundType.valueOf(roundType);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown roundType, fallback to default label: roundType={}", roundType);
            return "chấm lại";
        }
        return switch (round) {
            case INITIAL -> "chấm lại";
            case SPOT_CHECK -> "hậu kiểm và chấm lại";
            case REMEDIATION -> "xem xét lại";
            case APPEAL -> "chấm lại theo đơn phúc khảo";
        };
    }

    private String outcomeLabel(String outcome) {
        return "PASSED".equals(outcome) ? "Đạt" : "Chưa đạt";
    }

    private String formatScore(BigDecimal score) {
        return score == null ? "-" : score.toPlainString();
    }

    @DltHandler
    public void logDeadLetteredRecord(ConsumerRecord<String, String> record) {
        UUID eventId = KafkaEventHeaders.readEventIdOrNull(record);
        log.error(
            "assessment-result-lifecycle event sent to DLT: eventId={}, topic={}, partition={}, offset={}, payload={}",
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
