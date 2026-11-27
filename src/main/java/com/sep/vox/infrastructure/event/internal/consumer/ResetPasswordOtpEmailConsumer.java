package com.sep.vox.infrastructure.event.internal.consumer;

import java.time.Duration;
import java.time.Instant;
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
@RequiredArgsConstructor
@Slf4j
public class ResetPasswordOtpEmailConsumer {

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
        UUID eventId = KafkaEventHeaders.readEventId(record);

        log.info("Receive event reset-password-otp: eventId={}, partition={}, offset={}",
            eventId, record.partition(), record.offset());

        if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
            log.info("Skip processed event: eventId={}, consumer={}", eventId, CONSUMER_GROUP);
            ack.acknowledge();
            return;
        }

        ResetPasswordOtpRequestedPayloadV1 payload = parsePayload(record.value(), eventId);
        String otp = issueOtp(payload);
        sendOtpMail(payload, otp, eventId);
        markProcessed(eventId);

        // Never log the OTP or the recipient address at INFO level.
        log.info("Reset password OTP mail sent: eventId={}", eventId);
        ack.acknowledge();
    }

    private String issueOtp(ResetPasswordOtpRequestedPayloadV1 payload) {
        String otp = oneTimePasswordPort.generate(OTP_SIZE);
        cacheManagerPort.save(
            CacheKey.RESET_PASSWORD_PREFIX + CacheKey.OTP_PREFIX + payload.to(),
            oneTimePasswordPort.hash(otp),
            TTL);
        return otp;
    }

    private ResetPasswordOtpRequestedPayloadV1 parsePayload(String value, UUID eventId) {
        try {
            return jsonMapper.readValue(value, ResetPasswordOtpRequestedPayloadV1.class);
        } catch (Exception e) {
            // Not logging the record value here loses nothing -- the payload only has the email.
            log.error("Invalid payload: eventId={}", eventId, e);
            throw new IllegalStateException("Payload reset-password-otp không hợp lệ, eventId=" + eventId, e);
        }
    }

    private void sendOtpMail(ResetPasswordOtpRequestedPayloadV1 payload, String otp, UUID eventId) {
        try {
            String html = mailTemplatePort.renderResetPasswordOtpEmail(otp, EXPIRES_IN);
            mailSendingPort.sendHtml(payload.to(), SUBJECT, html);
        } catch (Exception e) {
            log.error("Send mail error: eventId={}", eventId, e);
            throw new IllegalStateException("Gửi mail OTP đặt lại mật khẩu thất bại: eventId=" + eventId, e);
        }
    }

    @DltHandler
    public void logDeadLetteredRecord(ConsumerRecord<String, String> record) {
        UUID eventId = KafkaEventHeaders.readEventIdOrNull(record);
        log.error("reset-password-otp event sent to DLT: eventId={}, topic={}, partition={}, offset={}",
            eventId, record.topic(), record.partition(), record.offset());
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
