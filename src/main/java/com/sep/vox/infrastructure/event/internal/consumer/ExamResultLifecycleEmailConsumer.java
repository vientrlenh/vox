package com.sep.vox.infrastructure.event.internal.consumer;

import java.math.BigDecimal;
import java.time.Instant;
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

import com.sep.vox.application.event.ExamResultInvalidClearedPayloadV1;
import com.sep.vox.application.event.ExamResultInvalidatedPayloadV1;
import com.sep.vox.application.event.ExamResultOutcomeDecidedPayloadV1;
import com.sep.vox.application.event.ExamResultRegradedPayloadV1;
import com.sep.vox.application.event.ExamResultReleasedPayloadV1;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.exam.GradingRoundType;
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
public class ExamResultLifecycleEmailConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExamResultLifecycleEmailConsumer.class);

    private static final String CONSUMER_GROUP = "exam-result-lifecycle-email";

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

    public ExamResultLifecycleEmailConsumer(
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
        topics = "${app.internal-event.kafka.consumer-groups.email.topic.exam-result-lifecycle}",
        groupId = "${app.internal-event.kafka.consumer-groups.email.group-id}",
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        var eventId = KafkaEventHeaders.readEventId(record);

        LOGGER.info("Receive event exam-result-lifecycle: eventId={}, partition={}, offset={}",
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
            throw new IllegalStateException("Gửi mail vòng đời điểm thất bại: eventId=" + eventId, e);
        }

        markProcessed(eventId);

        LOGGER.info("Exam result lifecycle mail sent: eventId={}, eventType={}, to={}",
            eventId, eventType, mail.to());
        ack.acknowledge();
    }

    /** @return {@code null} khi không tìm được người nhận */
    private Mail renderMail(String eventType, UUID eventId, String value) {
        return switch (eventType) {
            case EventTypeConstant.EXAM_RESULT_RELEASED -> {
                var payload = parse(value, ExamResultReleasedPayloadV1.class, eventType, eventId);
                yield mail(payload.studentId(), SUBJECT_RELEASED, () -> mailTemplatePort.renderResultReleasedEmail(
                    payload.examName(), formatScore(payload.totalScore())));
            }
            case EventTypeConstant.EXAM_RESULT_REGRADED -> {
                var payload = parse(value, ExamResultRegradedPayloadV1.class, eventType, eventId);
                yield mail(payload.studentId(), SUBJECT_REGRADED, () -> mailTemplatePort.renderResultRegradedEmail(
                    payload.examName(),
                    roundLabel(payload.roundType()),
                    formatScore(payload.scoreBefore()),
                    formatScore(payload.scoreAfter())));
            }
            case EventTypeConstant.EXAM_RESULT_INVALIDATED -> {
                var payload = parse(value, ExamResultInvalidatedPayloadV1.class, eventType, eventId);
                yield mail(payload.studentId(), SUBJECT_INVALIDATED,
                    () -> mailTemplatePort.renderResultInvalidatedEmail(payload.examName(), payload.reason()));
            }
            case EventTypeConstant.EXAM_RESULT_INVALID_CLEARED -> {
                var payload = parse(value, ExamResultInvalidClearedPayloadV1.class, eventType, eventId);
                yield mail(payload.studentId(), SUBJECT_INVALID_CLEARED,
                    () -> mailTemplatePort.renderResultInvalidClearedEmail(payload.examName(), payload.reason()));
            }
            case EventTypeConstant.EXAM_RESULT_OUTCOME_DECIDED -> {
                var payload = parse(value, ExamResultOutcomeDecidedPayloadV1.class, eventType, eventId);
                yield mail(payload.studentId(), SUBJECT_OUTCOME_DECIDED,
                    () -> mailTemplatePort.renderResultOutcomeDecidedEmail(
                        payload.examName(),
                        outcomeLabel(payload.outcome()),
                        formatScore(payload.totalScore())));
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
            LOGGER.warn("Unknown roundType, fallback to default label: roundType={}", roundType);
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
    public void handleDlt(ConsumerRecord<String, String> record) {
        var eventId = KafkaEventHeaders.readEventIdOrNull(record);
        LOGGER.error(
            "exam-result-lifecycle event sent to DLT: eventId={}, topic={}, partition={}, offset={}, payload={}",
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
