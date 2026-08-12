package com.sep.vox.infrastructure.event.internal.consumer;

import java.time.Duration;
import java.time.Instant;
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

import com.sep.vox.application.common.CacheKey;
import com.sep.vox.application.event.ResetPasswordOtpRequestedPayloadV1;
import com.sep.vox.application.port.output.CacheManagerPort;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;
import com.sep.vox.application.port.output.OneTimePasswordPort;
import com.sep.vox.domain.model.outbox.ProcessedEvent;
import com.sep.vox.domain.repository.ProcessedEventRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * Gửi OTP đặt lại mật khẩu.
 *
 * <p>OTP được sinh TẠI ĐÂY chứ không ở use case, theo đúng khuôn của
 * {@code UserCreatedEventConsumer}: OTP là credential, không được nằm trong
 * {@code outboxes.payload} (có backup) hay trong topic Kafka (giữ theo retention). Payload
 * chỉ nói "cần gửi OTP cho địa chỉ này".
 *
 * <p>Bản hash phải vào cache TRƯỚC khi gửi mail, nếu không người dùng nhập mã ngay có thể
 * gặp lúc cache chưa có gì để đối chiếu.
 *
 * <p>Retry sinh mã MỚI và ghi đè cache, nên mã ở mail lần trước hết hiệu lực. Đây là điểm
 * khác với token đặt mật khẩu (token cũ vẫn sống tới hạn): với OTP thì "mail mới nhất là
 * mail đúng" lại khớp với thứ người dùng chờ đợi.
 */
@Component
public class ResetPasswordOtpEmailConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResetPasswordOtpEmailConsumer.class);

    private static final String CONSUMER_GROUP = "reset-password-otp-email";
    private static final String SUBJECT = "Mã xác thực đặt lại mật khẩu VOX";
    private static final String EXPIRES_IN = "5 phút";

    private static final int OTP_SIZE = 7;
    private static final Duration TTL = Duration.ofMinutes(5);

    private final CacheManagerPort cacheManagerPort;
    private final OneTimePasswordPort oneTimePasswordPort;
    private final MailSendingPort mailSendingPort;
    private final MailTemplatePort mailTemplatePort;
    private final ProcessedEventRepository processedEventRepository;
    private final JsonMapper jsonMapper;

    public ResetPasswordOtpEmailConsumer(
            CacheManagerPort cacheManagerPort,
            OneTimePasswordPort oneTimePasswordPort,
            MailSendingPort mailSendingPort,
            MailTemplatePort mailTemplatePort,
            ProcessedEventRepository processedEventRepository,
            JsonMapper jsonMapper) {
        this.cacheManagerPort = cacheManagerPort;
        this.oneTimePasswordPort = oneTimePasswordPort;
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
        topics = "${app.internal-event.kafka.consumer-groups.email.topic.reset-password-otp}",
        groupId = "${app.internal-event.kafka.consumer-groups.email.group-id}",
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        var eventId = KafkaEventHeaders.readEventId(record);

        LOGGER.info("Receive event reset-password-otp: eventId={}, partition={}, offset={}",
            eventId, record.partition(), record.offset());

        if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
            LOGGER.info("Skip processed event: eventId={}, consumer={}", eventId, CONSUMER_GROUP);
            ack.acknowledge();
            return;
        }

        ResetPasswordOtpRequestedPayloadV1 payload;
        try {
            payload = jsonMapper.readValue(record.value(), ResetPasswordOtpRequestedPayloadV1.class);
        } catch (Exception e) {
            // Không log record.value() ở đây thì cũng không mất gì -- payload chỉ có email.
            LOGGER.error("Invalid payload: eventId={}", eventId, e);
            throw new IllegalStateException("Payload reset-password-otp không hợp lệ, eventId=" + eventId, e);
        }

        var otp = oneTimePasswordPort.generate(OTP_SIZE);
        cacheManagerPort.save(
            CacheKey.RESET_PASSWORD_PREFIX + CacheKey.OTP_PREFIX + payload.to(),
            oneTimePasswordPort.hash(otp),
            TTL);

        try {
            var html = mailTemplatePort.renderResetPasswordOtpEmail(otp, EXPIRES_IN);
            mailSendingPort.sendHtml(payload.to(), SUBJECT, html);
        } catch (Exception e) {
            LOGGER.error("Send mail error: eventId={}", eventId, e);
            throw new IllegalStateException("Gửi mail OTP đặt lại mật khẩu thất bại: eventId=" + eventId, e);
        }

        markProcessed(eventId);

        // Tuyệt đối không log OTP hay địa chỉ nhận ở mức INFO.
        LOGGER.info("Reset password OTP mail sent: eventId={}", eventId);
        ack.acknowledge();
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, String> record) {
        var eventId = KafkaEventHeaders.readEventIdOrNull(record);
        LOGGER.error("reset-password-otp event sent to DLT: eventId={}, topic={}, partition={}, offset={}",
            eventId, record.topic(), record.partition(), record.offset());
    }

    private void markProcessed(UUID eventId) {
        try {
            processedEventRepository.save(new ProcessedEvent(eventId, CONSUMER_GROUP, Instant.now()));
        } catch (DataIntegrityViolationException e) {
            LOGGER.warn("This event was already marked by another instance: eventId={}", eventId);
        }
    }
}
