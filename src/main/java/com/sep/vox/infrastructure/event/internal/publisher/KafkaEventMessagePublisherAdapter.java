package com.sep.vox.infrastructure.event.internal.publisher;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.EventMessagePublisherPort;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.infrastructure.properties.OutboxTopicProperties;

@Component
@Slf4j
public class KafkaEventMessagePublisherAdapter implements EventMessagePublisherPort {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Map<String, String> topicsByEventType;

    public KafkaEventMessagePublisherAdapter(
        @Qualifier("outboxKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate, 
        OutboxTopicProperties outboxTopicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicsByEventType = outboxTopicProperties.topics();
    }

    @Override
    public CompletableFuture<Void> publish(Outbox event) {
        String topic = topicsByEventType.get(event.getEventType());
        if (topic == null) {
            log.error("Topic not configured: eventId={}, eventType={}", event.getId(), event.getEventType());
            return CompletableFuture.failedFuture(
                new IllegalStateException("No topic configured for eventType=" + event.getEventType())
            );
        }

        ProducerRecord<String, String> record = new ProducerRecord<>(topic, event.getAggregateId().toString(), event.getPayload());
        record.headers().add("eventId", event.getId().toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add("eventType", event.getEventType().getBytes(StandardCharsets.UTF_8));

        try {
            return kafkaTemplate.send(record).thenApply(result -> null);
        } catch (Exception e) {
            log.error("Event publish failed: eventId={}, eventType={}, payload={}", event.getId(), event.getEventType(), event.getPayload());
            return CompletableFuture.failedFuture(e);
        }
    }
}
