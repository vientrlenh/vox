package com.sep.vox.infrastructure.event.internal.consumer;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class UserCreatedEventConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserCreatedEventConsumer.class);

    private static final String CONSUMER_GROUP = "user-created-email";
    private static final String SUBJECT = "Thiết lập mật khẩu tài khoản VOX";
    private static final String EXPIRES_IN = "48 giờ";

    private final MailSendingPort mailSendingPort;
    private final MailTemplatePort mailTemplatePort;
    private final PasswordSetUpTokenPort passwordSetUpTokenPort;
    private final PasswordSetUpTokenRepository passwordSetUpTokenRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final JsonMapper jsonMapper;

    @Value("${app.frontend.password-setup-url}")
    private String passwordSetupBaseUrl;

    public UserCreatedEventConsumer(
            MailSendingPort mailSendingPort,
            MailTemplatePort mailTemplatePort,
            PasswordSetUpTokenPort passwordSetUpTokenPort,
            PasswordSetUpTokenRepository passwordSetUpTokenRepository,
            ProcessedEventRepository processedEventRepository,
            JsonMapper jsonMapper) {
        this.mailSendingPort = mailSendingPort;
        this.mailTemplatePort = mailTemplatePort;
        this.passwordSetUpTokenPort = passwordSetUpTokenPort;
        this.passwordSetUpTokenRepository = passwordSetUpTokenRepository;
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
        topics = "${app.internal-event.kafka.consumer-groups.email.topic.user-created}",
        groupId = "${app.internal-event.kafka.consumer-groups.email.group-id}",
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        var eventId = KafkaEventHeaders.readEventId(record);

        LOGGER.info("Receive event user-created: eventId={}, partition={}, offset={}",
            eventId, record.partition(), record.offset());

        if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
            LOGGER.info("Skip processed event: eventId={}, consumer={}", eventId, CONSUMER_GROUP);
            ack.acknowledge();
            return;
        }

        UserCreatedPayloadV1 payload;
        try {
            payload = jsonMapper.readValue(record.value(), UserCreatedPayloadV1.class);
        } catch (Exception e) {
            LOGGER.error("Invalid payload: eventId={}, value={}", eventId, record.value(), e);
            throw new IllegalStateException("Payload user-created không hợp lệ, eventId=" + eventId, e);
        }

        // Sinh token TẠI ĐÂY thay vì ở producer: token là credential, không được nằm trong
        // payload outbox/Kafka. Phải lưu xong (commit) trước khi gửi mail, nếu không người
        // dùng bấm link ngay có thể gặp token chưa tồn tại trong DB.
        //
        // Retry sẽ sinh token mới -- không tái sử dụng được token cũ vì DB chỉ giữ bản hash.
        // Token cũ vẫn hợp lệ tới khi hết hạn 48h; chấp nhận được, và số lần retry có trần.
        String rawToken;
        try {
            var generatedToken = passwordSetUpTokenPort.generateToken();
            passwordSetUpTokenRepository.save(
                PasswordSetUpToken.create(payload.userId(), generatedToken.hashedToken()));
            rawToken = generatedToken.rawToken();
        } catch (Exception e) {
            LOGGER.error("Generate password set up token failed: eventId={}, userId={}",
                eventId, payload.userId(), e);
            throw new IllegalStateException(
                "Sinh token đặt mật khẩu thất bại: eventId=" + eventId, e);
        }

        try {
            var setupUrl = buildPasswordSetupUrl(payload.userId(), rawToken);
            var html = renderEmail(payload, setupUrl);
            mailSendingPort.sendHtml(payload.to(), SUBJECT, html);
        } catch (Exception e) {
            LOGGER.error("Send mail error: eventId={}, to={}", eventId, payload.to(), e);
            throw new IllegalStateException("Gửi mail thiết lập mật khẩu thất bại: eventId=" + eventId, e);
        }

        markProcessed(eventId);

        LOGGER.info("Password set up mail sent: eventId={}, to={}, userType={}",
            eventId, payload.to(), payload.userType());
        ack.acknowledge();
    }

    private String renderEmail(UserCreatedPayloadV1 payload, String setupUrl) {
        return switch (payload.userType()) {
            case UserTypeConstant.SCHOOL_ADMIN -> mailTemplatePort.renderPasswordSetUpEmail(
                payload.fullName(), payload.schoolName(), setupUrl, EXPIRES_IN);
            case UserTypeConstant.SCHOOL_USER -> mailTemplatePort.renderSchoolUserPasswordSetUpEmail(
                payload.fullName(), payload.schoolName(), setupUrl, EXPIRES_IN);
            case null, default -> throw new IllegalStateException(
                "userType không hợp lệ: " + payload.userType());
        };
    }

    private String buildPasswordSetupUrl(UUID userId, String rawToken) {
        var separator = passwordSetupBaseUrl.contains("?") ? "&" : "?";
        return passwordSetupBaseUrl + separator
            + "userId=" + encode(userId.toString())
            + "&token=" + encode(rawToken);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, String> record) {
        var eventId = KafkaEventHeaders.readEventIdOrNull(record);
        LOGGER.error("user-created event sent to DLT: eventId={}, topic={}, partition={}, offset={}, payload={}",
            eventId, record.topic(), record.partition(), record.offset(), record.value());
    }

    private void markProcessed(UUID eventId) {
        try {
            processedEventRepository.save(new ProcessedEvent(eventId, CONSUMER_GROUP, OffsetDateTime.now()));
        } catch (DataIntegrityViolationException e) {
            LOGGER.warn("This event was already marked by another instance: eventId={}", eventId);
        }
    }
}
