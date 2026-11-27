package com.sep.vox.infrastructure.event.internal.consumer;

import com.sep.vox.domain.model.user.User;
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
import com.sep.vox.application.event.GradingAssignmentDeclinedPayloadV1;
import com.sep.vox.application.event.GradingDeadlineReminderPayloadV1;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.model.outbox.ProcessedEvent;
import com.sep.vox.domain.repository.ProcessedEventRepository;
import com.sep.vox.domain.repository.UserRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * Email for the <em>operations</em> side: reminding a teacher whose grading is about to be due, and telling the admin when
 * someone gives an assignment back.
 *
 * <p>Kept apart from {@link AssessmentResultLifecycleEmailConsumer} because the recipients are completely different --
 * here they are school staff, not students; mixing them easily leads to sending to the wrong group.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GradingAssignmentEmailConsumer {

    private static final String CONSUMER_GROUP = "grading-assignment-email";

    private static final String SUBJECT_DEADLINE_REMINDER = "Grading deadline reminder";
    private static final String SUBJECT_DECLINED = "A teacher returned a grading assignment";

    private static final String PLACEHOLDER = "-";

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
        topics = "${app.internal-event.kafka.consumer-groups.email.topic.grading-assignment}",
        groupId = "${app.internal-event.kafka.consumer-groups.email.group-id}",
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        UUID eventId = KafkaEventHeaders.readEventId(record);

        log.info("Receive event grading-assignment: eventId={}, partition={}, offset={}",
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
     * If the recipient has been deleted, resending is pointless: mark it processed
     * and skip, do not let the message run the whole retry cycle and end up in the DLT.
     */
    private void skipMissingRecipient(UUID eventId, String eventType, Acknowledgment ack) {
        markProcessed(eventId);
        log.warn("Skip mail, recipient not found: eventId={}, eventType={}", eventId, eventType);
        ack.acknowledge();
    }

    private void deliverMail(Mail mail, UUID eventId, String eventType) {
        sendMail(mail, eventId);
        markProcessed(eventId);

        log.info("Grading assignment mail sent: eventId={}, eventType={}, to={}",
            eventId, eventType, mail.to());
    }

    private void sendMail(Mail mail, UUID eventId) {
        try {
            mailSendingPort.sendHtml(mail.to(), mail.subject(), mail.html());
        } catch (Exception e) {
            log.error("Send mail error: eventId={}, to={}", eventId, mail.to(), e);
            throw new IllegalStateException("Failed to send the grading assignment email: eventId=" + eventId, e);
        }
    }

    /** @return {@code null} when no recipient can be found */
    private Mail renderMail(String eventType, UUID eventId, String value) {
        return switch (eventType) {
            case EventTypeConstant.GRADING_DEADLINE_REMINDER -> {
                GradingDeadlineReminderPayloadV1 payload = parse(value, GradingDeadlineReminderPayloadV1.class, eventType, eventId);
                yield mail(payload.teacherId(), SUBJECT_DEADLINE_REMINDER,
                    () -> mailTemplatePort.renderGradingDeadlineReminderEmail(
                        orPlaceholder(payload.assessmentName()),
                        roundLabel(payload.roundType()),
                        formatDeadline(payload.deadlineAt())));
            }
            // The recipient is the admin who ASSIGNED the attempt, not the student: the attempt is waiting to be reassigned.
            case EventTypeConstant.GRADING_ASSIGNMENT_DECLINED -> {
                GradingAssignmentDeclinedPayloadV1 payload = parse(value, GradingAssignmentDeclinedPayloadV1.class, eventType, eventId);
                yield mail(payload.assignedBy(), SUBJECT_DECLINED,
                    () -> mailTemplatePort.renderGradingDeclinedEmail(
                        orPlaceholder(payload.assessmentName()),
                        teacherName(payload.teacherId()),
                        orPlaceholder(payload.reason())));
            }
            default -> throw new IllegalStateException(
                "Invalid eventType: " + eventType + ", eventId=" + eventId);
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
            log.error("Invalid payload: eventId={}, eventType={}, value={}", eventId, eventType, value, e);
            throw new IllegalStateException(
                "Payload " + eventType + " is invalid, eventId=" + eventId, e);
        }
    }

    /**
     * An unknown value returns a default label instead of throwing: an old message still in the topic may
     * carry a round name that has since been renamed or deleted, not worth sending the whole email to the DLT.
     */
    private String roundLabel(String roundType) {
        if (roundType == null) {
            return PLACEHOLDER;
        }
        GradingRoundType round;
        try {
            round = GradingRoundType.valueOf(roundType);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown roundType, fallback to default label: roundType={}", roundType);
            return PLACEHOLDER;
        }
        return switch (round) {
            case INITIAL -> "Initial grading";
            case SPOT_CHECK -> "Spot check";
            case REMEDIATION -> "Review of invalidated submission";
            case APPEAL -> "Appeal";
        };
    }

    private String orPlaceholder(String value) {
        return value == null ? PLACEHOLDER : value;
    }

    private String formatDeadline(Instant deadline) {
        return deadline == null ? PLACEHOLDER : DEADLINE_FORMAT.format(deadline);
    }

    @DltHandler
    public void logDeadLetteredRecord(ConsumerRecord<String, String> record) {
        UUID eventId = KafkaEventHeaders.readEventIdOrNull(record);
        log.error(
            "grading-assignment event sent to DLT: eventId={}, topic={}, partition={}, offset={}, payload={}",
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
