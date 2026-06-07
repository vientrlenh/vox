package com.sep.vox.infrastructure.event.publisher;

import java.util.Locale;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.ExternalEventPublisherPort;
import com.sep.vox.infrastructure.config.ExternalEventKafkaProperties;
import com.sep.vox.infrastructure.exception.InfrastructureException;

@Component
@ConditionalOnProperty(prefix = "spring.external-events", name = "provider", havingValue = "kafka")
public class KafkaExternalEventPublisher implements ExternalEventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ExternalEventKafkaProperties properties;

    public KafkaExternalEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            ExternalEventKafkaProperties properties) {
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
        var eventName = event.getClass().getSimpleName();
        var configuredTopic = properties.getTopics().get(eventName);
        if (hasText(configuredTopic)) {
            return configuredTopic;
        }

        var defaultTopic = toKebabCase(eventName);
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
