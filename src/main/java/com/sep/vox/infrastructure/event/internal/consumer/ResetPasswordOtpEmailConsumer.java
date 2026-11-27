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
 * Sends the OTP for password reset.
 *
 * <p>The OTP is generated HERE and not in the use case, following the same mould as
 * {@code UserCreatedEventConsumer}: an OTP is a credential and must not sit in
 * {@code outboxes.payload} (which is backed up) or in the Kafka topic (kept per retention). The payload
 * only says "an OTP needs to be sent to this address".
 *
 * <p>The hash must enter the cache BEFORE the mail is sent, otherwise a user entering the code right away may
 * find the cache has nothing to compare against yet.
 *
 * <p>A retry generates a NEW code and overwrites the cache, so the code in the previous mail stops working. This is a difference
 * from the password-setting token (the old token stays alive until expiry): for an OTP, "the newest mail is
 * the correct mail" matches what the user is waiting for.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResetPasswordOtpEmailConsumer {

    private static final String CONSUMER_GROUP = "reset-password-otp-email";
    private static final String SUBJECT = "VOX password reset verification code";
    private static final String EXPIRES_IN = "5 minutes";

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
            throw new IllegalStateException("Payload reset-password-otp is invalid, eventId=" + eventId, e);
        }
    }

    private void sendOtpMail(ResetPasswordOtpRequestedPayloadV1 payload, String otp, UUID eventId) {
        try {
            String html = mailTemplatePort.renderResetPasswordOtpEmail(otp, EXPIRES_IN);
            mailSendingPort.sendHtml(payload.to(), SUBJECT, html);
        } catch (Exception e) {
            log.error("Send mail error: eventId={}", eventId, e);
            throw new IllegalStateException("Failed to send the password reset OTP email: eventId=" + eventId, e);
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
