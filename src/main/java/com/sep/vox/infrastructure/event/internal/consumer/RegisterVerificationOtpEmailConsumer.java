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
import com.sep.vox.application.common.CachePayload;
import com.sep.vox.application.event.RegisterVerificationOtpRequestedPayloadV1;
import com.sep.vox.application.port.output.CacheManagerPort;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;
import com.sep.vox.application.port.output.OneTimePasswordPort;
import com.sep.vox.domain.model.outbox.ProcessedEvent;
import com.sep.vox.domain.repository.ProcessedEventRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * Gửi OTP xác thực đăng ký trường.
 *
 * <p>OTP sinh tại đây, không nằm trong payload -- xem
 * {@link ResetPasswordOtpEmailConsumer} về lý do.
 *
 * <p>Khác với đặt lại mật khẩu, cache của luồng này giữ cả hồ sơ đăng ký chứ không chỉ mã
 * hash, và hồ sơ đó do use case ghi. Nên ở đây phải đọc dòng cache ra, chèn hash rồi ghi
 * lại. Nếu dòng cache đã hết hạn thì người dùng đã bỏ cuộc từ lâu: bỏ qua chứ không retry.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RegisterVerificationOtpEmailConsumer {

    private static final String CONSUMER_GROUP = "register-verification-otp-email";
    private static final String SUBJECT = "Mã xác thực đăng ký VOX";
    private static final String EXPIRES_IN = "10 phút";

    private static final int OTP_SIZE = 6;

    /** Counted again from the moment the email is sent, not from when the form was submitted -- that is the moment the user sees. */
    private static final Duration TTL = Duration.ofMinutes(10);

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
        topics = "${app.internal-event.kafka.consumer-groups.email.topic.register-verification-otp}",
        groupId = "${app.internal-event.kafka.consumer-groups.email.group-id}",
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        UUID eventId = KafkaEventHeaders.readEventId(record);

        log.info("Receive event register-verification-otp: eventId={}, partition={}, offset={}",
            eventId, record.partition(), record.offset());

        if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
            log.info("Skip processed event: eventId={}, consumer={}", eventId, CONSUMER_GROUP);
            ack.acknowledge();
            return;
        }

        RegisterVerificationOtpRequestedPayloadV1 payload = parsePayload(record.value(), eventId);

        String key = CacheKey.registerVerificationKey(payload.to());
        CachePayload.RegisterVerificationPayload cached = cacheManagerPort.get(key, CachePayload.RegisterVerificationPayload.class);
        if (cached == null) {
            skipExpiredRegistration(eventId, ack);
            return;
        }

        String otp = issueOtp(key, cached);
        deliverOtpMail(payload, otp, eventId);
        ack.acknowledge();
    }

    private void skipExpiredRegistration(UUID eventId, Acknowledgment ack) {
        log.warn("Bỏ qua OTP đăng ký, hồ sơ trong cache đã hết hạn: eventId={}", eventId);
        markProcessed(eventId);
        ack.acknowledge();
    }

    private String issueOtp(String key, CachePayload.RegisterVerificationPayload cached) {
        String otp = oneTimePasswordPort.generate(OTP_SIZE);
        cacheManagerPort.save(key, withOtpHash(cached, oneTimePasswordPort.hash(otp)), TTL);
        return otp;
    }

    private void deliverOtpMail(RegisterVerificationOtpRequestedPayloadV1 payload, String otp, UUID eventId) {
        sendOtpMail(payload, otp, eventId);
        markProcessed(eventId);

        log.info("Register verification OTP mail sent: eventId={}", eventId);
    }

    private CachePayload.RegisterVerificationPayload withOtpHash(
            CachePayload.RegisterVerificationPayload source, String otpHash) {
        return new CachePayload.RegisterVerificationPayload(
            otpHash,
            source.email(),
            source.schoolDirectoryId(),
            source.fullName(),
            source.identityNumber(),
            source.phone(),
            source.dateOfBirth(),
            source.address(),
            source.postalCode(),
            source.position(),
            source.studentCount()
        );
    }

    private RegisterVerificationOtpRequestedPayloadV1 parsePayload(String value, UUID eventId) {
        try {
            return jsonMapper.readValue(value, RegisterVerificationOtpRequestedPayloadV1.class);
        } catch (Exception e) {
            log.error("Invalid payload: eventId={}", eventId, e);
            throw new IllegalStateException(
                "Payload register-verification-otp không hợp lệ, eventId=" + eventId, e);
        }
    }

    private void sendOtpMail(RegisterVerificationOtpRequestedPayloadV1 payload, String otp, UUID eventId) {
        try {
            String html = mailTemplatePort.renderRegisterVerificationOtpEmail(otp, EXPIRES_IN);
            mailSendingPort.sendHtml(payload.to(), SUBJECT, html);
        } catch (Exception e) {
            log.error("Send mail error: eventId={}", eventId, e);
            throw new IllegalStateException("Gửi mail OTP đăng ký thất bại: eventId=" + eventId, e);
        }
    }

    @DltHandler
    public void logDeadLetteredRecord(ConsumerRecord<String, String> record) {
        UUID eventId = KafkaEventHeaders.readEventIdOrNull(record);
        log.error("register-verification-otp event sent to DLT: eventId={}, topic={}, partition={}, offset={}",
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
