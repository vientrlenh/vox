package com.sep.vox.infrastructure.event.external.producer;

import java.util.Locale;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.sep.vox.application.event.ExternalEventTopic;
import com.sep.vox.application.port.output.ExternalEventPublisherPort;
import com.sep.vox.infrastructure.config.ExternalEventProperties;
import com.sep.vox.infrastructure.exception.InfrastructureException;

@Component
public class KafkaEventProducer implements ExternalEventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ExternalEventProperties properties;

    public KafkaEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            ExternalEventProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Override
    public void publish(Object event) {
        if (event == null) {
            throw new IllegalArgumentException("External event khong duoc de trong");
        }

        var topic = resolveTopic(event);
        try {
            kafkaTemplate.send(topic, event.getClass().getSimpleName(), event);
        } catch (Exception exception) {
            throw new InfrastructureException("Khong the publish external event len Kafka: " + exception.getMessage());
        }
    }

    private String resolveTopic(Object event) {
        // Ưu tiên: annotation trên class
        var annotation = event.getClass().getAnnotation(ExternalEventTopic.class);
        if (annotation != null) {
            return annotation.value();
        }

        // Fallback: auto-generate
        var defaultTopic = toKebabCase(event.getClass().getSimpleName());
        if (!hasText(properties.getTopicPrefix())) {
            return defaultTopic;
        }

        return properties.getTopicPrefix().strip().replaceAll("[.]+$", "") + "." + defaultTopic;
    }

    private String toKebabCase(String input) {
        return input
            .replaceAll("([a-z0-9])([A-Z])", "$1-$2")
            .toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
