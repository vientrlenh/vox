package com.sep.vox.infrastructure.event.internal.consumer;

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

import com.sep.vox.application.event.RegisterFormRejectedPayloadV1;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;
import com.sep.vox.domain.model.outbox.ProcessedEvent;
import com.sep.vox.domain.repository.ProcessedEventRepository;

import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class RejectRegisterFormEventConsumer {

    private static final String CONSUMER_GROUP = "reject-register-form-email";
    private static final String SUBJECT = "VOX registration result notification";
    
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
        topics = "${app.internal-event.kafka.consumer-groups.email.topic.register-form-rejected}", 
        groupId = "${app.internal-event.kafka.consumer-groups.email.group-id}", 
        containerFactory= "stringKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        UUID eventId = KafkaEventHeaders.readEventId(record);

        log.info("Receive event register-form-rejected: eventId={}, parition={}, offset={}", eventId, record.partition(), record.offset());

        if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
            log.info("Skip processed event: eventId={}, consumer={}", eventId, CONSUMER_GROUP);
            ack.acknowledge();
            return;
        }

        RegisterFormRejectedPayloadV1 payload = parsePayload(record.value(), eventId);
        sendRejectionMail(payload, eventId);
        markProcessed(eventId);

        log.info("Reject register form mail sent: eventId={}, to={}", eventId, payload.to());
        ack.acknowledge();
    }

    private RegisterFormRejectedPayloadV1 parsePayload(String value, UUID eventId) {
        try {
            return jsonMapper.readValue(value, RegisterFormRejectedPayloadV1.class);
        } catch (Exception e) {
            log.error("Invalid payload: eventId={}, value={}", eventId, value);
            throw new IllegalStateException("Payload register-form-rejected is invalid, eventId= " + eventId, e);
        }
    }

    private void sendRejectionMail(RegisterFormRejectedPayloadV1 payload, UUID eventId) {
        try {
            String html = mailTemplatePort.renderRejectRegisterFormEmail(payload.reason());
            mailSendingPort.sendHtml(payload.to(), SUBJECT, html);
        } catch (Exception e) {
            log.error("Send mail error: eventId={}, to={}", eventId, payload.to());
            throw new IllegalStateException("Failed to send the registration rejection email: eventId=" + eventId, e);
        }
    }

    @DltHandler
    public void logDeadLetteredRecord(ConsumerRecord<String, String> record) {
        UUID eventId = KafkaEventHeaders.readEventIdOrNull(record);
        log.error("register-form-reject event sent to DLT: eventId={}, topic={}, payload={}", eventId, record.topic(), record.value());
    }

    private void markProcessed(UUID eventId) {
        try {
            ProcessedEvent event = ProcessedEvent.builder()
                    .eventId(eventId)
                    .consumerGroup(CONSUMER_GROUP)
                    .processedAt(Instant.now())
                    .build();
            processedEventRepository.save(event);
        } catch (DataIntegrityViolationException e) {
            log.warn("This event was already marked by another instance: eventId={}", eventId);
        }
    }
}
