package com.sep.vox.infrastructure.event.external;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.sep.vox.application.event.ExternalEventTopic;
import com.sep.vox.application.event.HasPartitionKey;
import com.sep.vox.application.port.output.ExternalEventPublisherPort;
import com.sep.vox.infrastructure.exception.InfrastructureException;

@Component
public class KafkaEventProducerAdapter implements ExternalEventPublisherPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventProducerAdapter.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventProducerAdapter(
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(Object event) {
        if (event == null) {
            throw new IllegalArgumentException("Loại event external không được để trống");
        }

        var topic = resolveTopic(event);
        var eventType = event.getClass().getSimpleName();
        var key = event instanceof HasPartitionKey keyed ? keyed.partitionKey() : eventType;
        LOGGER.info("Publishing event {} to kafka", eventType);
        try {
            kafkaTemplate.send(topic, key, event).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Interrupted when sending event: {}", e);
            throw new InfrastructureException("Bị gián đoạn khi gửi event");
        }
        catch (ExecutionException e) {
            LOGGER.error("Event published failed to kafka: {}", e);
            throw new InfrastructureException("Có lỗi xảy ra, không thể gửi event lên kafka");
        }
    }

    private String resolveTopic(Object event) {
        var annotation = event.getClass().getAnnotation(ExternalEventTopic.class);
        return annotation == null ? toKebabCase(event.getClass().getSimpleName()) : annotation.value();
    }

    private String toKebabCase(String input) {
        return input.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase(Locale.ROOT);
    }

}
