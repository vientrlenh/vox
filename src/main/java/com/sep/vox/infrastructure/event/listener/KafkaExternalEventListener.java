package com.sep.vox.infrastructure.event.listener;

import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.vox.application.port.input.ExternalEventHandler;

@Component
public class KafkaExternalEventListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaExternalEventListener.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final List<ExternalEventHandler> handlers;

    public KafkaExternalEventListener(List<ExternalEventHandler> handlers) {
        this.handlers = handlers;
    }

    @KafkaListener(
        topics = "${app.external-events.consumer-groups.user-service.topics}",
        groupId = "${app.external-events.consumer-groups.user-service.group-id}"
    )
    public void onUserEvent(ConsumerRecord<String, Object> record) {
        dispatch(record);
    }

    @KafkaListener(
        topics = "${app.external-events.consumer-groups.notification-service.topics}",
        groupId = "${app.external-events.consumer-groups.notification-service.group-id}"
    )
    public void onNotificationEvent(ConsumerRecord<String, Object> record) {
        dispatch(record);
    }

    @KafkaListener(
        topics = "${app.external-events.consumer-groups.audit-service.topics}",
        groupId = "${app.external-events.consumer-groups.audit-service.group-id}"
    )
    public void onAuditEvent(ConsumerRecord<String, Object> record) {
        dispatch(record);
    }

    private void dispatch(ConsumerRecord<String, Object> record) {
        String eventType = record.key();
        Object value = record.value();
        JsonNode payload = OBJECT_MAPPER.valueToTree(value);

        log.info("Nhan external event: type={}, topic={}, partition={}, offset={}",
            eventType, record.topic(), record.partition(), record.offset());

        for (ExternalEventHandler handler : handlers) {
            try {
                handler.handle(eventType, payload);
            } catch (Exception exception) {
                log.error("Loi khi xu ly external event type={}: {}", eventType, exception.getMessage(), exception);
            }
        }
    }
}
