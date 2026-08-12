package com.sep.vox.infrastructure.event.internal.consumer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sep.vox.domain.model.subscription.SubscriptionQuota;
import com.sep.vox.domain.model.subscription.TokenPurchaseItem;
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
import com.sep.vox.application.event.InvoicePaidPayloadV1;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;
import com.sep.vox.domain.model.outbox.ProcessedEvent;
import com.sep.vox.domain.model.subscription.InvoiceSourceType;
import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.repository.ProcessedEventRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;
import com.sep.vox.domain.repository.TokenPurchaseItemRepository;
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
public class InvoiceEmailConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvoiceEmailConsumer.class);

    private static final String CONSUMER_GROUP = "invoice-email";

    private static final Map<QuotaType, String> QUOTA_LABELS = Map.of(
        QuotaType.GRADING, "Bài thi cần chấm",
        QuotaType.CLASS_TEST, "Bài kiểm tra trên lớp",
        QuotaType.PRACTICE, "Lượt ôn luyện cá nhân"
    );

    // DateTimeFormatter an toàn dùng chung giữa các thread; DecimalFormat thì không, nên nó
    // được tạo mới trong mỗi lần gọi consume() thay vì giữ làm field static.
    private static final DateTimeFormatter PAID_AT_FORMATTER =
        DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(DateMapper.DEFAULT_INPUT_ZONE);
    private static final DateTimeFormatter VALID_UNTIL_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SchoolRepository schoolRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionQuotaRepository subscriptionQuotaRepository;
    private final TokenPurchaseItemRepository tokenPurchaseItemRepository;
    private final UserRepository userRepository;
    private final MailSendingPort mailSendingPort;
    private final MailTemplatePort mailTemplatePort;
    private final ProcessedEventRepository processedEventRepository;
    private final JsonMapper jsonMapper;

    public InvoiceEmailConsumer(
            SchoolRepository schoolRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            SubscriptionQuotaRepository subscriptionQuotaRepository,
            TokenPurchaseItemRepository tokenPurchaseItemRepository,
            UserRepository userRepository,
            MailSendingPort mailSendingPort,
            MailTemplatePort mailTemplatePort,
            ProcessedEventRepository processedEventRepository,
            JsonMapper jsonMapper) {
        this.schoolRepository = schoolRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.tokenPurchaseItemRepository = tokenPurchaseItemRepository;
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
        topics = "${app.internal-event.kafka.consumer-groups.email.topic.invoice}",
        groupId = "${app.internal-event.kafka.consumer-groups.email.group-id}",
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        var eventId = KafkaEventHeaders.readEventId(record);

        LOGGER.info("Receive event invoice: eventId={}, partition={}, offset={}",
            eventId, record.partition(), record.offset());

        if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
            LOGGER.info("Skip processed event: eventId={}, consumer={}", eventId, CONSUMER_GROUP);
            ack.acknowledge();
            return;
        }

        var payload = parse(record.value(), eventId);

        if (payload.schoolAdminIds() == null || payload.schoolAdminIds().isEmpty()) {
            LOGGER.warn("Skip mail, trường không còn admin nào: eventId={}, schoolId={}",
                eventId, payload.schoolId());
            markProcessed(eventId);
            ack.acknowledge();
            return;
        }

        var html = renderHtml(payload, eventId);
        var subject = "Hóa đơn thanh toán #" + payload.invoiceNumber();

        // Một địa chỉ hỏng không được chặn những người còn lại: gom lỗi rồi ném MỘT lần ở
        // cuối. Retry sẽ gửi lại cho cả nhóm, nhưng mail trùng đỡ tệ hơn mail thiếu.
        var failed = 0;
        for (var recipientId : payload.schoolAdminIds()) {
            var recipient = userRepository.findById(recipientId).orElse(null);
            if (recipient == null) {
                LOGGER.warn("Bỏ qua người nhận không còn tồn tại: eventId={}, userId={}", eventId, recipientId);
                continue;
            }
            try {
                mailSendingPort.sendHtml(recipient.getEmail().value(), subject, html);
            } catch (Exception e) {
                failed++;
                LOGGER.error("Send mail error: eventId={}, to={}", eventId, recipient.getEmail().value(), e);
            }
        }

        if (failed > 0) {
            throw new IllegalStateException(
                "Gửi mail hóa đơn thất bại cho " + failed + " người nhận: eventId=" + eventId);
        }

        markProcessed(eventId);

        LOGGER.info("Invoice mail sent: eventId={}, invoiceNumber={}, recipients={}",
            eventId, payload.invoiceNumber(), payload.schoolAdminIds().size());
        ack.acknowledge();
    }

    private String renderHtml(InvoicePaidPayloadV1 payload, UUID eventId) {
        var school = schoolRepository.findById(payload.schoolId())
            .orElseThrow(() -> new IllegalStateException(
                "Không tìm thấy trường: eventId=" + eventId + ", schoolId=" + payload.schoolId()));
        var subscription = schoolSubscriptionRepository.findById(payload.subscriptionId())
            .orElseThrow(() -> new IllegalStateException(
                "Không tìm thấy gói đăng ký: eventId=" + eventId));
        var plan = subscriptionPlanRepository.findById(subscription.getPlanId())
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy gói: eventId=" + eventId));

        String itemTitle;
        String itemsHtml;
        if (payload.sourceType() == InvoiceSourceType.TOKEN_PURCHASE) {
            itemTitle = "Mua thêm hạn mức";
            var items = tokenPurchaseItemRepository.findAllByPurchaseId(payload.sourceId());
            itemsHtml = buildItemsHtml(items.stream()
                .collect(Collectors.toMap(TokenPurchaseItem::getQuotaType, TokenPurchaseItem::getQuantity)));
        } else {
            itemTitle = "Gói " + plan.getName();
            var quotas = subscriptionQuotaRepository.findAllBySubscriptionId(subscription.getId());
            itemsHtml = buildItemsHtml(quotas.stream()
                .collect(Collectors.toMap(SubscriptionQuota::getQuotaType, SubscriptionQuota::getTotalAllocated)));
        }

        var amountFormatter = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.of("vi", "VN")));
        var amountLabel = amountFormatter.format(payload.amount()) + " ₫";
        var paidAtLabel = payload.paidAt() == null ? "-" : PAID_AT_FORMATTER.format(payload.paidAt());
        var validUntilLabel = subscription.getEndDate() == null
            ? "-" : VALID_UNTIL_FORMATTER.format(subscription.getEndDate());

        return mailTemplatePort.renderInvoicePaidEmail(
            school.getName(), payload.invoiceNumber(), itemTitle, itemsHtml,
            amountLabel, paidAtLabel, validUntilLabel);
    }

    // Quota tính bằng USD chi phí AI (xem V15__quota_unit_to_usd.sql), hiển thị 2 chữ số thập phân.
    private String buildItemsHtml(Map<QuotaType, BigDecimal> quantityByType) {
        var rows = new StringBuilder();
        for (var quotaType : QuotaType.values()) {
            var quantity = quantityByType.get(quotaType);
            if (quantity == null) {
                continue;
            }
            var usdLabel = quantity.setScale(2, RoundingMode.HALF_UP).toPlainString();
            rows.append("<tr><td style=\"padding:6px 20px; color:#475569; font-size:14px;\">")
                .append(QUOTA_LABELS.get(quotaType))
                .append("</td><td style=\"padding:6px 20px; text-align:right; font-weight:700; font-size:14px; color:#1e293b;\">")
                .append(usdLabel)
                .append(" USD</td></tr>");
        }
        return rows.toString();
    }

    private InvoicePaidPayloadV1 parse(String value, UUID eventId) {
        try {
            return jsonMapper.readValue(value, InvoicePaidPayloadV1.class);
        } catch (Exception e) {
            LOGGER.error("Invalid payload: eventId={}, value={}", eventId, value, e);
            throw new IllegalStateException("Payload invoice không hợp lệ, eventId=" + eventId, e);
        }
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, String> record) {
        var eventId = KafkaEventHeaders.readEventIdOrNull(record);
        LOGGER.error("invoice event sent to DLT: eventId={}, topic={}, partition={}, offset={}, payload={}",
            eventId, record.topic(), record.partition(), record.offset(), record.value());
    }

    private void markProcessed(UUID eventId) {
        try {
            processedEventRepository.save(new ProcessedEvent(eventId, CONSUMER_GROUP, Instant.now()));
        } catch (DataIntegrityViolationException e) {
            LOGGER.warn("This event was already marked by another instance: eventId={}", eventId);
        }
    }
}
