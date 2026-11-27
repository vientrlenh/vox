package com.sep.vox.infrastructure.event.internal.consumer;

import com.sep.vox.domain.model.user.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

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

import com.sep.vox.application.event.SchoolDebtCapExceededPayloadV1;
import com.sep.vox.application.event.SchoolDebtClearedPayloadV1;
import com.sep.vox.application.event.SchoolLockedDueToDebtPayloadV1;
import com.sep.vox.application.event.SchoolSubscriptionSuspendedPayloadV1;
import com.sep.vox.application.event.SchoolSubscriptionUnsuspendedPayloadV1;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.outbox.ProcessedEvent;
import com.sep.vox.domain.repository.ProcessedEventRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * Emails for the 3 AI quota debt events (section 13, AI_USAGE_QUOTA_USD_MIGRATION.md), mirroring the way
 * {@link InvoiceEmailConsumer} separates the mail consumer group from the in-app consumer group
 * ({@link NotificationPushedEventConsumer}) on the SAME topic {@code school-debt}. Recipients are taken
 * straight from the payload (fixed at publish time), not queried again.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SchoolDebtEmailConsumer {

    private static final String CONSUMER_GROUP = "school-debt-email";

    /** DecimalFormat is NOT thread-safe, so this must be a ThreadLocal and not a shared field. */
    private static final ThreadLocal<DecimalFormat> AMOUNT_FORMAT = ThreadLocal.withInitial(
        () -> new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.of("vi", "VN"))));

    private final SchoolRepository schoolRepository;
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
        topics = "${app.internal-event.kafka.consumer-groups.email.topic.school-debt}",
        groupId = "${app.internal-event.kafka.consumer-groups.email.group-id}",
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        UUID eventId = KafkaEventHeaders.readEventId(record);

        log.info("Receive event school-debt: eventId={}, partition={}, offset={}",
            eventId, record.partition(), record.offset());

        if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
            log.info("Skip processed event: eventId={}, consumer={}", eventId, CONSUMER_GROUP);
            ack.acknowledge();
            return;
        }

        String eventType = KafkaEventHeaders.readEventType(record);

        Mail mail = renderMail(eventType, record.value(), eventId);

        if (mail.recipientIds() == null || mail.recipientIds().isEmpty()) {
            skipWithoutRecipients(eventId, eventType, ack);
            return;
        }

        deliverMail(mail, eventId, eventType);
        ack.acknowledge();
    }

    private void skipWithoutRecipients(UUID eventId, String eventType, Acknowledgment ack) {
        log.warn("Skip mail, no recipient: eventId={}, eventType={}", eventId, eventType);
        markProcessed(eventId);
        ack.acknowledge();
    }

    private void deliverMail(Mail mail, UUID eventId, String eventType) {
        int failed = sendToRecipients(mail, eventId);

        if (failed > 0) {
            throw new IllegalStateException(
                "Failed to send the quota debt email to " + failed + " recipients: eventId=" + eventId);
        }

        markProcessed(eventId);

        log.info("School-debt mail sent: eventId={}, eventType={}, recipients={}",
            eventId, eventType, mail.recipientIds().size());
    }

    /**
     * One bad address must not block the others: collect the errors and let the caller throw ONCE at the end,
     * the same way InvoiceEmailConsumer does it.
     *
     * @return the number of recipients the mail could not be sent to
     */
    private int sendToRecipients(Mail mail, UUID eventId) {
        int failed = 0;
        for (UUID recipientId : mail.recipientIds()) {
            User recipient = userRepository.findById(recipientId).orElse(null);
            if (recipient == null) {
                log.warn("Skipping a recipient that no longer exists: eventId={}, userId={}", eventId, recipientId);
                continue;
            }
            try {
                mailSendingPort.sendHtml(recipient.getEmail().value(), mail.subject(), mail.html());
            } catch (Exception e) {
                failed++;
                log.error("Send mail error: eventId={}, to={}", eventId, recipient.getEmail().value(), e);
            }
        }
        return failed;
    }

    private Mail renderMail(String eventType, String value, UUID eventId) {
        return switch (eventType) {
            case EventTypeConstant.SCHOOL_DEBT_CAP_EXCEEDED -> renderCapExceededMail(value, eventId);
            case EventTypeConstant.SCHOOL_LOCKED_DUE_TO_DEBT -> renderLockedMail(value, eventId);
            case EventTypeConstant.SCHOOL_DEBT_CLEARED -> renderClearedMail(value, eventId);
            case EventTypeConstant.SCHOOL_SUBSCRIPTION_SUSPENDED -> renderSuspendedMail(value, eventId);
            case EventTypeConstant.SCHOOL_SUBSCRIPTION_UNSUSPENDED -> renderUnsuspendedMail(value, eventId);
            default -> throw new IllegalStateException(
                "Unhandled eventType: eventId=" + eventId + ", eventType=" + eventType);
        };
    }

    private Mail renderCapExceededMail(String value, UUID eventId) {
        SchoolDebtCapExceededPayloadV1 payload = parse(value, SchoolDebtCapExceededPayloadV1.class, eventId);
        return new Mail(
            payload.systemAdminIds(),
            "Warning: AI quota debt exceeds the cap",
            renderCapExceeded(payload, eventId)
        );
    }

    private Mail renderLockedMail(String value, UUID eventId) {
        SchoolLockedDueToDebtPayloadV1 payload = parse(value, SchoolLockedDueToDebtPayloadV1.class, eventId);
        return new Mail(
            payload.schoolAdminIds(),
            "The school is locked due to AI quota debt",
            mailTemplatePort.renderSchoolLockedDueToDebtEmail(schoolNameOf(payload.schoolId(), eventId))
        );
    }

    private Mail renderClearedMail(String value, UUID eventId) {
        SchoolDebtClearedPayloadV1 payload = parse(value, SchoolDebtClearedPayloadV1.class, eventId);
        return new Mail(
            payload.schoolAdminIds(),
            "The school has cleared its AI quota debt",
            mailTemplatePort.renderSchoolDebtClearedEmail(schoolNameOf(payload.schoolId(), eventId))
        );
    }

    private Mail renderSuspendedMail(String value, UUID eventId) {
        SchoolSubscriptionSuspendedPayloadV1 payload = parse(value, SchoolSubscriptionSuspendedPayloadV1.class, eventId);
        return new Mail(
            payload.schoolAdminIds(),
            "Subscription plan suspended",
            mailTemplatePort.renderSchoolSubscriptionSuspendedEmail(
                schoolNameOf(payload.schoolId(), eventId), payload.reason())
        );
    }

    private Mail renderUnsuspendedMail(String value, UUID eventId) {
        SchoolSubscriptionUnsuspendedPayloadV1 payload = parse(value, SchoolSubscriptionUnsuspendedPayloadV1.class, eventId);
        return new Mail(
            payload.schoolAdminIds(),
            "Subscription plan unsuspended",
            mailTemplatePort.renderSchoolSubscriptionUnsuspendedEmail(schoolNameOf(payload.schoolId(), eventId))
        );
    }

    @DltHandler
    public void logDeadLetteredRecord(ConsumerRecord<String, String> record) {
        UUID eventId = KafkaEventHeaders.readEventIdOrNull(record);
        log.error("school-debt event sent to DLT: eventId={}, topic={}, partition={}, offset={}, payload={}",
            eventId, record.topic(), record.partition(), record.offset(), record.value());
    }

    private String renderCapExceeded(SchoolDebtCapExceededPayloadV1 payload, UUID eventId) {
        return mailTemplatePort.renderDebtCapExceededEmail(
            schoolNameOf(payload.schoolId(), eventId),
            quotaLabel(payload.quotaType()),
            formatVnd(payload.overageVnd()),
            formatVnd(payload.capVnd())
        );
    }

    private String schoolNameOf(UUID schoolId, UUID eventId) {
        return schoolRepository.findById(schoolId)
            .map(school -> school.getName())
            .orElseThrow(() -> new IllegalStateException(
                "School not found: eventId=" + eventId + ", schoolId=" + schoolId));
    }

    private String quotaLabel(QuotaType quotaType) {
        if (quotaType == null) {
            return "--";
        }
        return switch (quotaType) {
            case ASSESSMENT -> "Assessment";
            case PRACTICE -> "Personal practice session";
        };
    }

    // Round to the DONG before showing: debt is stored at scale 6 so no fractions are lost when accumulating,
    // but "12.345,678901 ₫" in an email sent to the admin is just hard to read.
    private String formatVnd(BigDecimal amountVnd) {
        return amountVnd == null
            ? "--"
            : AMOUNT_FORMAT.get().format(amountVnd.setScale(0, RoundingMode.HALF_UP)) + " ₫";
    }

    private <T> T parse(String value, Class<T> type, UUID eventId) {
        try {
            return jsonMapper.readValue(value, type);
        } catch (Exception e) {
            throw new IllegalStateException("Malformed payload: eventId=" + eventId, e);
        }
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

    private record Mail(List<UUID> recipientIds, String subject, String html) {
    }
}
