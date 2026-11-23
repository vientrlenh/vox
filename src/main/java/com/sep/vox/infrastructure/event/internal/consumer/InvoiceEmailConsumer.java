package com.sep.vox.infrastructure.event.internal.consumer;

import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.user.User;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
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

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.event.InvoicePaidPayloadV1;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;
import com.sep.vox.domain.model.invoice.InvoiceSourceType;
import com.sep.vox.domain.model.outbox.ProcessedEvent;
import com.sep.vox.domain.repository.ProcessedEventRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.repository.UserRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * Mail báo hóa đơn đã thu tiền, gửi cho mọi SCHOOL_ADMIN của trường.
 *
 * <p>Thay cho {@code InvoicePaidEmailListener} cũ chạy in-process sau commit. Phần dựng nội
 * dung giữ nguyên; cái đổi là nó nay nằm sau outbox nên mất tiến trình không mất thông báo,
 * và lỗi ở đây không còn dội ngược thành 500 cho webhook cổng thanh toán.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceEmailConsumer {

    private static final String CONSUMER_GROUP = "invoice-email";

    // DateTimeFormatter is safe to share between threads; DecimalFormat is not, so it is
    // created anew on each consume() call instead of being kept as a static field.
    private static final DateTimeFormatter PAID_AT_FORMATTER =
        DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(DateMapper.DEFAULT_INPUT_ZONE);
    private static final DateTimeFormatter VALID_UNTIL_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SchoolRepository schoolRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
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
        topics = "${app.internal-event.kafka.consumer-groups.email.topic.invoice}",
        groupId = "${app.internal-event.kafka.consumer-groups.email.group-id}",
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        UUID eventId = KafkaEventHeaders.readEventId(record);

        log.info("Receive event invoice: eventId={}, partition={}, offset={}",
            eventId, record.partition(), record.offset());

        if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
            log.info("Skip processed event: eventId={}, consumer={}", eventId, CONSUMER_GROUP);
            ack.acknowledge();
            return;
        }

        InvoicePaidPayloadV1 payload = parse(record.value(), eventId);

        if (payload.schoolAdminIds() == null || payload.schoolAdminIds().isEmpty()) {
            log.warn("Skip mail, trường không còn admin nào: eventId={}, schoolId={}",
                eventId, payload.schoolId());
            markProcessed(eventId);
            ack.acknowledge();
            return;
        }

        String html = renderHtml(payload, eventId);
        String subject = "Hóa đơn thanh toán #" + payload.invoiceNumber();

        // One bad address must not block the others: collect the errors and throw ONCE at the
        // end. The retry will resend to the whole group, but a duplicate mail is better than a missing one.
        int failed = 0;
        for (UUID recipientId : payload.schoolAdminIds()) {
            User recipient = userRepository.findById(recipientId).orElse(null);
            if (recipient == null) {
                log.warn("Bỏ qua người nhận không còn tồn tại: eventId={}, userId={}", eventId, recipientId);
                continue;
            }
            try {
                mailSendingPort.sendHtml(recipient.getEmail().value(), subject, html);
            } catch (Exception e) {
                failed++;
                log.error("Send mail error: eventId={}, to={}", eventId, recipient.getEmail().value(), e);
            }
        }

        if (failed > 0) {
            throw new IllegalStateException(
                "Gửi mail hóa đơn thất bại cho " + failed + " người nhận: eventId=" + eventId);
        }

        markProcessed(eventId);

        log.info("Invoice mail sent: eventId={}, invoiceNumber={}, recipients={}",
            eventId, payload.invoiceNumber(), payload.schoolAdminIds().size());
        ack.acknowledge();
    }

    private String renderHtml(InvoicePaidPayloadV1 payload, UUID eventId) {
        School school = schoolRepository.findById(payload.schoolId())
            .orElseThrow(() -> new IllegalStateException(
                "Không tìm thấy trường: eventId=" + eventId + ", schoolId=" + payload.schoolId()));
        boolean isTopUp = payload.sourceType() == InvoiceSourceType.TOPUP;

        // A quota top-up NOT tied to any subscription period: a school that has never bought a plan can still top up,
        // and then the publisher sends a null subscriptionId (see OrderSettlementService
        // .creditBalance). Insisting on a period here would throw, the message would run the whole retry cycle
        // and land in the DLT -- the receipt mail of every top-up of that kind would never arrive.
        SchoolSubscription subscription = payload.subscriptionId() == null
            ? null
            : schoolSubscriptionRepository.findById(payload.subscriptionId()).orElse(null);

        // A PLAN invoice whose period cannot be looked up must still fail loudly as before: that is corrupt data, not
        // a valid shape.
        if (subscription == null && !isTopUp) {
            throw new IllegalStateException("Không tìm thấy gói đăng ký: eventId=" + eventId);
        }

        String itemTitle;
        if (isTopUp) {
            itemTitle = "Mua thêm hạn mức";
        } else {
            // The subscription null check is required here, the analyzer flags subscription.getSubscriptionPlanId() as a possible null reference
            if (subscription == null) {
                throw new IllegalStateException("Không tìm thấy gói đăng ký: eventId=" + eventId);
            }
            SubscriptionPlan plan = subscriptionPlanRepository.findById(subscription.getSubscriptionPlanId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy gói: eventId=" + eventId));
            itemTitle = "Gói " + plan.getName();
        }

        DecimalFormat amountFormatter = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.of("vi", "VN")));
        String amountLabel = amountFormatter.format(payload.amount()) + " ₫";
        String paidAtLabel = payload.paidAt() == null ? "-" : PAID_AT_FORMATTER.format(payload.paidAt());
        String validUntilLabel = subscription == null || subscription.getEndDate() == null
            ? "-" : VALID_UNTIL_FORMATTER.format(subscription.getEndDate());

        return mailTemplatePort.renderInvoicePaidEmail(
            school.getName(), payload.invoiceNumber(), itemTitle,
            amountLabel, paidAtLabel, validUntilLabel);
    }

    private InvoicePaidPayloadV1 parse(String value, UUID eventId) {
        try {
            return jsonMapper.readValue(value, InvoicePaidPayloadV1.class);
        } catch (Exception e) {
            log.error("Invalid payload: eventId={}, value={}", eventId, value, e);
            throw new IllegalStateException("Payload invoice không hợp lệ, eventId=" + eventId, e);
        }
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, String> record) {
        UUID eventId = KafkaEventHeaders.readEventIdOrNull(record);
        log.error("invoice event sent to DLT: eventId={}, topic={}, partition={}, offset={}, payload={}",
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
}
