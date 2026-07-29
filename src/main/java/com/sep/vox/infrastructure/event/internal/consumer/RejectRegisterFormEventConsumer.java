package com.sep.vox.infrastructure.event.internal.consumer;

import java.time.OffsetDateTime;
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

import com.sep.vox.application.event.RegisterFormRejectedPayloadV1;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;
import com.sep.vox.domain.model.outbox.ProcessedEvent;
import com.sep.vox.domain.repository.ProcessedEventRepository;

import tools.jackson.databind.json.JsonMapper;

@Component
public class RejectRegisterFormEventConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RejectRegisterFormEventConsumer.class);

    private static final String CONSUMER_GROUP = "reject-register-form-email";
    private static final String SUBJECT = "Thông báo kết quả đăng ký VOX";
    
    private final MailSendingPort mailSendingPort;
    private final MailTemplatePort mailTemplatePort;
    private final ProcessedEventRepository processedEventRepository;
    private final JsonMapper jsonMapper;

    public RejectRegisterFormEventConsumer(MailSendingPort mailSendingPort, MailTemplatePort mailTemplatePort, ProcessedEventRepository processedEventRepository, JsonMapper jsonMapper) {
        this.mailSendingPort = mailSendingPort;
        this.mailTemplatePort = mailTemplatePort;
        this.processedEventRepository = processedEventRepository; 
        this.jsonMapper = jsonMapper;
    }

    @RetryableTopic(
        attempts = "4", 
        backOff = @BackOff(delay = 2000, multiplier = 2.0, maxDelay = 30000), 
        autoCreateTopics = "true", 
        dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(
        topics = "${app.internal-event.kafka.consumer-groups.email.topic.register-form-rejected}", 
        groupId = "${app.internal-event.kafka.consumer-groups.email.group-id}", 
        containerFactory= "stringKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        var eventId = KafkaEventHeaders.readEventId(record);

        LOGGER.info("Receive event register-form-rejected: eventId={}, parition={}, offset={}", eventId, record.partition(), record.offset());

        if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
            LOGGER.info("Skip processed event: eventId={}, consumer={}", eventId, CONSUMER_GROUP);
            ack.acknowledge();
            return;
        }

        RegisterFormRejectedPayloadV1 payload;
        try {
            payload = jsonMapper.readValue(record.value(), RegisterFormRejectedPayloadV1.class);
        } catch (Exception e) {
            LOGGER.error("Invalid payload: eventId={}, value={}", eventId, record.value());
            throw new IllegalStateException("Payload register-form-rejected không hợp lệ, eventId= " + eventId, e);
        }

        try {
            var html = mailTemplatePort.renderRejectRegisterFormEmail(payload.reason());
            mailSendingPort.sendHtml(payload.to(), SUBJECT, html);
        } catch (Exception e) {
            LOGGER.error("Send mail error: eventId={}, to={}", eventId, payload.to());
            throw new IllegalStateException("Gửi mail từ chối đơn đăng ký thất bại: eventId=" + eventId, e);
        }

        markProcessed(eventId);

        LOGGER.info("Reject register form mail sent: eventId={}, to={}", eventId, payload.to());
        ack.acknowledge();
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, String> record) {
        var eventId = KafkaEventHeaders.readEventIdOrNull(record);
        LOGGER.error("register-form-reject event sent to DLT: eventId={}, topic={}, payload={}", eventId, record.topic(), record.value());
    }

    private void markProcessed(UUID eventId) {
        try {
            var event = new ProcessedEvent(eventId, CONSUMER_GROUP, OffsetDateTime.now());
            processedEventRepository.save(event);
        } catch (DataIntegrityViolationException e) {
            LOGGER.warn("This event was already marked by another instance: eventId={}", eventId);
        }
    }
}
