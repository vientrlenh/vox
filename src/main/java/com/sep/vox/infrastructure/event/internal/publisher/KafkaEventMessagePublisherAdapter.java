package com.sep.vox.infrastructure.event.internal.publisher;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.EventMessagePublisherPort;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.infrastructure.properties.OutboxTopicProperties;

@Component
public class KafkaEventMessagePublisherAdapter implements EventMessagePublisherPort {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Map<String, String> topicsByEventType;

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventMessagePublisherAdapter.class);

    public KafkaEventMessagePublisherAdapter(
        @Qualifier("outboxKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate, 
        OutboxTopicProperties outboxTopicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicsByEventType = outboxTopicProperties.topics();
    }

    @Override
    public CompletableFuture<Void> publish(Outbox event) {
        var topic = topicsByEventType.get(event.getEventType());
        if (topic == null) {
            LOGGER.error("Topic not configured: eventId={}, eventType={}", event.getId(), event.getEventType());
            return CompletableFuture.failedFuture(
                new IllegalStateException("Chưa cấu hình topic cho eventType=" + event.getEventType())
            );
        }

        var record = new ProducerRecord<>(topic, event.getAggregateId().toString(), event.getPayload());
        record.headers().add("eventId", event.getId().toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add("eventType", event.getEventType().getBytes(StandardCharsets.UTF_8));

        try {
            return kafkaTemplate.send(record).thenApply(result -> null);
        } catch (Exception e) {
            LOGGER.error("Event publish failed: eventId={}, eventType={}, payload={}", event.getId(), event.getEventType(), event.getPayload());
            return CompletableFuture.failedFuture(e);
        }
    }
}
