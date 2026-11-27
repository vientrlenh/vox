package com.sep.vox.infrastructure.event.internal.consumer;

import com.sep.vox.application.response.output.GeneratedPasswordSetUpToken;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.sep.vox.application.event.UserCreatedPayloadV1;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;
import com.sep.vox.application.port.output.PasswordSetUpTokenPort;
import com.sep.vox.domain.common.UserTypeConstant;
import com.sep.vox.domain.model.outbox.ProcessedEvent;
import com.sep.vox.domain.model.passwordsetuptoken.PasswordSetUpToken;
import com.sep.vox.domain.repository.PasswordSetUpTokenRepository;
import com.sep.vox.domain.repository.ProcessedEventRepository;

import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserCreatedEventConsumer {

    private static final String CONSUMER_GROUP = "user-created-email";
    private static final String SUBJECT = "VOX account password setup";
    private static final String EXPIRES_IN = "48 hours";

    private final MailSendingPort mailSendingPort;
    private final MailTemplatePort mailTemplatePort;
    private final PasswordSetUpTokenPort passwordSetUpTokenPort;
    private final PasswordSetUpTokenRepository passwordSetUpTokenRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final JsonMapper jsonMapper;

    @Value("${app.frontend.password-setup-url}")
    private String passwordSetupBaseUrl;

    @RetryableTopic(
        attempts = "4",
        backOff = @BackOff(delay = 2000, multiplier = 2.0, maxDelay = 30000),
        autoCreateTopics = "true",
        dltStrategy = DltStrategy.FAIL_ON_ERROR, 
        kafkaTemplate = "outboxKafkaTemplate"
    )
    @KafkaListener(
        topics = "${app.internal-event.kafka.consumer-groups.email.topic.user-created}",
        groupId = "${app.internal-event.kafka.consumer-groups.email.group-id}",
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        UUID eventId = KafkaEventHeaders.readEventId(record);

        log.info("Receive event user-created: eventId={}, partition={}, offset={}",
            eventId, record.partition(), record.offset());

        if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
            log.info("Skip processed event: eventId={}, consumer={}", eventId, CONSUMER_GROUP);
            ack.acknowledge();
            return;
        }

        UserCreatedPayloadV1 payload = parsePayload(record.value(), eventId);
        String rawToken = createPasswordSetUpToken(payload, eventId);
        sendPasswordSetUpMail(payload, rawToken, eventId);
        markProcessed(eventId);

        log.info("Password set up mail sent: eventId={}, to={}, userType={}",
            eventId, payload.to(), payload.userType());
        ack.acknowledge();
    }

    private UserCreatedPayloadV1 parsePayload(String value, UUID eventId) {
        try {
            return jsonMapper.readValue(value, UserCreatedPayloadV1.class);
        } catch (Exception e) {
            log.error("Invalid payload: eventId={}, value={}", eventId, value, e);
            throw new IllegalStateException("Payload user-created is invalid, eventId=" + eventId, e);
        }
    }

    // Generate the token HERE instead of in the producer: the token is a credential and must not be in the
    // outbox/Kafka payload. It must be saved (committed) before the mail is sent, otherwise a user
    // who clicks the link right away may hit a token that does not exist in the DB yet.
    //
    // A retry generates a new token -- the old one cannot be reused because the DB only keeps the hash.
    // The old token stays valid until it expires after 48h; acceptable, and the number of retries is capped.
    private String createPasswordSetUpToken(UserCreatedPayloadV1 payload, UUID eventId) {
        try {
            GeneratedPasswordSetUpToken generatedToken = passwordSetUpTokenPort.generateToken();
            passwordSetUpTokenRepository.save(
                PasswordSetUpToken.create(payload.userId(), generatedToken.hashedToken()));
            return generatedToken.rawToken();
        } catch (Exception e) {
            log.error("Generate password set up token failed: eventId={}, userId={}",
                eventId, payload.userId(), e);
            throw new IllegalStateException(
                "Failed to generate the password-setting token: eventId=" + eventId, e);
        }
    }

    private void sendPasswordSetUpMail(UserCreatedPayloadV1 payload, String rawToken, UUID eventId) {
        try {
            String setupUrl = buildPasswordSetupUrl(payload.userId(), rawToken);
            String html = renderEmail(payload, setupUrl);
            mailSendingPort.sendHtml(payload.to(), SUBJECT, html);
        } catch (Exception e) {
            log.error("Send mail error: eventId={}, to={}", eventId, payload.to(), e);
            throw new IllegalStateException("Failed to send the password setup email: eventId=" + eventId, e);
        }
    }

    private String renderEmail(UserCreatedPayloadV1 payload, String setupUrl) {
        return switch (payload.userType()) {
            case UserTypeConstant.SCHOOL_ADMIN -> mailTemplatePort.renderPasswordSetUpEmail(
                payload.fullName(), payload.schoolName(), setupUrl, EXPIRES_IN);
            case UserTypeConstant.SCHOOL_USER -> mailTemplatePort.renderSchoolUserPasswordSetUpEmail(
                payload.fullName(), payload.schoolName(), setupUrl, EXPIRES_IN);
            case null, default -> throw new IllegalStateException(
                "Invalid userType: " + payload.userType());
        };
    }

    private String buildPasswordSetupUrl(UUID userId, String rawToken) {
        String separator = passwordSetupBaseUrl.contains("?") ? "&" : "?";
        return passwordSetupBaseUrl + separator
            + "userId=" + encode(userId.toString())
            + "&token=" + encode(rawToken);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @DltHandler
    public void logDeadLetteredRecord(ConsumerRecord<String, String> record) {
        UUID eventId = KafkaEventHeaders.readEventIdOrNull(record);
        log.error("user-created event sent to DLT: eventId={}, topic={}, partition={}, offset={}, payload={}",
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
