package com.sep.vox.infrastructure.event.internal.consumer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

import com.sep.vox.application.event.SchoolDebtCapExceededPayloadV1;
import com.sep.vox.application.event.SchoolDebtClearedPayloadV1;
import com.sep.vox.application.event.SchoolLockedDueToDebtPayloadV1;
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
 * Mail cho 3 sự kiện nợ hạn mức AI (mục 13, AI_USAGE_QUOTA_USD_MIGRATION.md), mirror đúng cách
 * {@link InvoiceEmailConsumer} tách riêng consumer group mail khỏi consumer group in-app
 * ({@link NotificationPushedEventConsumer}) trên CÙNG topic {@code school-debt}. Người nhận lấy
 * thẳng từ payload (đã chốt lúc publish), không truy vấn lại.
 */
@Component
public class SchoolDebtEmailConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchoolDebtEmailConsumer.class);

    private static final String CONSUMER_GROUP = "school-debt-email";

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final MailSendingPort mailSendingPort;
    private final MailTemplatePort mailTemplatePort;
    private final ProcessedEventRepository processedEventRepository;
    private final JsonMapper jsonMapper;

    public SchoolDebtEmailConsumer(
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            MailSendingPort mailSendingPort,
            MailTemplatePort mailTemplatePort,
            ProcessedEventRepository processedEventRepository,
            JsonMapper jsonMapper) {
        this.schoolRepository = schoolRepository;
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
        topics = "${app.internal-event.kafka.consumer-groups.email.topic.school-debt}",
        groupId = "${app.internal-event.kafka.consumer-groups.email.group-id}",
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        var eventId = KafkaEventHeaders.readEventId(record);

        LOGGER.info("Receive event school-debt: eventId={}, partition={}, offset={}",
            eventId, record.partition(), record.offset());

        if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
            LOGGER.info("Skip processed event: eventId={}, consumer={}", eventId, CONSUMER_GROUP);
            ack.acknowledge();
            return;
        }

        var eventType = KafkaEventHeaders.readEventType(record);

        var mail = switch (eventType) {
            case EventTypeConstant.SCHOOL_DEBT_CAP_EXCEEDED -> {
                var payload = parse(record.value(), SchoolDebtCapExceededPayloadV1.class, eventId);
                yield new Mail(
                    payload.systemAdminIds(),
                    "Cảnh báo: nợ hạn mức AI vượt trần",
                    renderCapExceeded(payload, eventId)
                );
            }
            case EventTypeConstant.SCHOOL_LOCKED_DUE_TO_DEBT -> {
                var payload = parse(record.value(), SchoolLockedDueToDebtPayloadV1.class, eventId);
                yield new Mail(
                    payload.schoolAdminIds(),
                    "Trường đang bị khóa do nợ hạn mức AI",
                    mailTemplatePort.renderSchoolLockedDueToDebtEmail(schoolNameOf(payload.schoolId(), eventId))
                );
            }
            case EventTypeConstant.SCHOOL_DEBT_CLEARED -> {
                var payload = parse(record.value(), SchoolDebtClearedPayloadV1.class, eventId);
                yield new Mail(
                    payload.schoolAdminIds(),
                    "Trường đã hết nợ hạn mức AI",
                    mailTemplatePort.renderSchoolDebtClearedEmail(schoolNameOf(payload.schoolId(), eventId))
                );
            }
            default -> throw new IllegalStateException(
                "eventType không được xử lý: eventId=" + eventId + ", eventType=" + eventType);
        };

        if (mail.recipientIds() == null || mail.recipientIds().isEmpty()) {
            LOGGER.warn("Skip mail, không có người nhận: eventId={}, eventType={}", eventId, eventType);
            markProcessed(eventId);
            ack.acknowledge();
            return;
        }

        // Một địa chỉ hỏng không được chặn những người còn lại: gom lỗi rồi ném MỘT lần ở cuối,
        // cùng cách InvoiceEmailConsumer đang làm.
        var failed = 0;
        for (var recipientId : mail.recipientIds()) {
            var recipient = userRepository.findById(recipientId).orElse(null);
            if (recipient == null) {
                LOGGER.warn("Bỏ qua người nhận không còn tồn tại: eventId={}, userId={}", eventId, recipientId);
                continue;
            }
            try {
                mailSendingPort.sendHtml(recipient.getEmail().value(), mail.subject(), mail.html());
            } catch (Exception e) {
                failed++;
                LOGGER.error("Send mail error: eventId={}, to={}", eventId, recipient.getEmail().value(), e);
            }
        }

        if (failed > 0) {
            throw new IllegalStateException(
                "Gửi mail nợ hạn mức thất bại cho " + failed + " người nhận: eventId=" + eventId);
        }

        markProcessed(eventId);

        LOGGER.info("School-debt mail sent: eventId={}, eventType={}, recipients={}",
            eventId, eventType, mail.recipientIds().size());
        ack.acknowledge();
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, String> record) {
        var eventId = KafkaEventHeaders.readEventIdOrNull(record);
        LOGGER.error("school-debt event sent to DLT: eventId={}, topic={}, partition={}, offset={}, payload={}",
            eventId, record.topic(), record.partition(), record.offset(), record.value());
    }

    private String renderCapExceeded(SchoolDebtCapExceededPayloadV1 payload, UUID eventId) {
        return mailTemplatePort.renderDebtCapExceededEmail(
            schoolNameOf(payload.schoolId(), eventId),
            quotaLabel(payload.quotaType()),
            formatUsd(payload.overageUsd()),
            formatUsd(payload.capUsd())
        );
    }

    private String schoolNameOf(UUID schoolId, UUID eventId) {
        return schoolRepository.findById(schoolId)
            .map(school -> school.getName())
            .orElseThrow(() -> new IllegalStateException(
                "Không tìm thấy trường: eventId=" + eventId + ", schoolId=" + schoolId));
    }

    private String quotaLabel(QuotaType quotaType) {
        if (quotaType == null) {
            return "--";
        }
        return switch (quotaType) {
            case EXAM -> "Bài kiểm tra";
            case PRACTICE -> "Lượt ôn luyện cá nhân";
        };
    }

    private String formatUsd(BigDecimal amountUsd) {
        return amountUsd == null ? "--" : "$" + amountUsd.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private <T> T parse(String value, Class<T> type, UUID eventId) {
        try {
            return jsonMapper.readValue(value, type);
        } catch (Exception e) {
            throw new IllegalStateException("Payload sai định dạng: eventId=" + eventId, e);
        }
    }

    private void markProcessed(UUID eventId) {
        try {
            processedEventRepository.save(new ProcessedEvent(eventId, CONSUMER_GROUP, Instant.now()));
        } catch (DataIntegrityViolationException e) {
            LOGGER.warn("This event was already marked by another instance: eventId={}", eventId);
        }
    }

    private record Mail(List<UUID> recipientIds, String subject, String html) {
    }
}
