package com.sep.vox.infrastructure.event.internal.consumer;

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
import com.sep.vox.application.event.GradingAssignmentDeclinedPayloadV1;
import com.sep.vox.application.event.GradingDeadlineReminderPayloadV1;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.model.outbox.ProcessedEvent;
import com.sep.vox.domain.repository.ProcessedEventRepository;
import com.sep.vox.domain.repository.UserRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * Mail cho phía <em>vận hành</em>: nhắc giáo viên sắp tới hạn, và báo admin khi có
 * người trả lại phân công.
 *
 * <p>Tách khỏi {@link ExamResultLifecycleEmailConsumer} vì người nhận khác hẳn — ở đây
 * là nhân sự của trường, không phải học sinh; trộn chung dễ dẫn tới gửi nhầm nhóm.
 */
@Component
public class GradingAssignmentEmailConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GradingAssignmentEmailConsumer.class);

    private static final String CONSUMER_GROUP = "grading-assignment-email";

    private static final String SUBJECT_DEADLINE_REMINDER = "Nhắc hạn chấm bài";
    private static final String SUBJECT_DECLINED = "Có giáo viên trả lại phân công chấm bài";

    private static final String PLACEHOLDER = "-";

    /** {@code withZone} là phần bắt buộc: thiếu nó thì mail ghi giờ UTC của container. */
    private static final DateTimeFormatter DEADLINE_FORMAT =
        DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(DateMapper.DEFAULT_INPUT_ZONE);

    private final UserRepository userRepository;
    private final MailSendingPort mailSendingPort;
    private final MailTemplatePort mailTemplatePort;
    private final ProcessedEventRepository processedEventRepository;
    private final JsonMapper jsonMapper;

    public GradingAssignmentEmailConsumer(
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
        topics = "${app.internal-event.kafka.consumer-groups.email.topic.grading-assignment}",
        groupId = "${app.internal-event.kafka.consumer-groups.email.group-id}",
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        var eventId = KafkaEventHeaders.readEventId(record);

        LOGGER.info("Receive event grading-assignment: eventId={}, partition={}, offset={}",
            eventId, record.partition(), record.offset());

        if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
            LOGGER.info("Skip processed event: eventId={}, consumer={}", eventId, CONSUMER_GROUP);
            ack.acknowledge();
            return;
        }

        var eventType = KafkaEventHeaders.readEventType(record);
        var mail = renderMail(eventType, eventId, record.value());

        // Người nhận đã bị xoá thì gửi lại bao nhiêu lần cũng vô ích: đánh dấu đã xử lý
        // rồi bỏ qua, đừng để message chạy hết vòng retry rồi rơi vào DLT.
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
            throw new IllegalStateException("Gửi mail phân công chấm bài thất bại: eventId=" + eventId, e);
        }

        markProcessed(eventId);

        LOGGER.info("Grading assignment mail sent: eventId={}, eventType={}, to={}",
            eventId, eventType, mail.to());
        ack.acknowledge();
    }

    /** @return {@code null} khi không tìm được người nhận */
    private Mail renderMail(String eventType, UUID eventId, String value) {
        return switch (eventType) {
            case EventTypeConstant.GRADING_DEADLINE_REMINDER -> {
                var payload = parse(value, GradingDeadlineReminderPayloadV1.class, eventType, eventId);
                yield mail(payload.teacherId(), SUBJECT_DEADLINE_REMINDER,
                    () -> mailTemplatePort.renderGradingDeadlineReminderEmail(
                        orPlaceholder(payload.examName()),
                        roundLabel(payload.roundType()),
                        formatDeadline(payload.deadlineAt())));
            }
            // Người nhận là admin ĐÃ GIAO bài, không phải học sinh: bài đang chờ giao lại.
            case EventTypeConstant.GRADING_ASSIGNMENT_DECLINED -> {
                var payload = parse(value, GradingAssignmentDeclinedPayloadV1.class, eventType, eventId);
                yield mail(payload.assignedBy(), SUBJECT_DECLINED,
                    () -> mailTemplatePort.renderGradingDeclinedEmail(
                        orPlaceholder(payload.examName()),
                        teacherName(payload.teacherId()),
                        orPlaceholder(payload.reason())));
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

    private String teacherName(UUID teacherId) {
        if (teacherId == null) {
            return PLACEHOLDER;
        }
        return userRepository.findById(teacherId)
            .map(user -> user.getFullName().value())
            .orElse(PLACEHOLDER);
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
     * Giá trị lạ trả về nhãn mặc định thay vì ném: message cũ còn nằm trong topic có thể
     * mang tên vòng đã bị đổi hoặc xoá, không đáng để đẩy cả mail vào DLT.
     */
    private String roundLabel(String roundType) {
        if (roundType == null) {
            return PLACEHOLDER;
        }
        GradingRoundType round;
        try {
            round = GradingRoundType.valueOf(roundType);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unknown roundType, fallback to default label: roundType={}", roundType);
            return PLACEHOLDER;
        }
        return switch (round) {
            case INITIAL -> "Chấm lần đầu";
            case SPOT_CHECK -> "Hậu kiểm";
            case REMEDIATION -> "Xem xét bài bị vô hiệu";
            case APPEAL -> "Phúc khảo";
        };
    }

    private String orPlaceholder(String value) {
        return value == null ? PLACEHOLDER : value;
    }

    private String formatDeadline(Instant deadline) {
        return deadline == null ? PLACEHOLDER : DEADLINE_FORMAT.format(deadline);
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, String> record) {
        var eventId = KafkaEventHeaders.readEventIdOrNull(record);
        LOGGER.error(
            "grading-assignment event sent to DLT: eventId={}, topic={}, partition={}, offset={}, payload={}",
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
