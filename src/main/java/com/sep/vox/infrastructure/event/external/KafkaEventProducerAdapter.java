package com.sep.vox.infrastructure.event.external;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.sep.vox.application.event.ExternalEventTopic;
import com.sep.vox.application.event.HasPartitionKey;
import com.sep.vox.application.port.output.ExternalEventPublisherPort;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventProducerAdapter implements ExternalEventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(Object event) {
        if (event == null) {
            throw new IllegalArgumentException("Loại event external không được để trống");
        }

        String topic = resolveTopic(event);
        String eventType = event.getClass().getSimpleName();
        String key = event instanceof HasPartitionKey keyed ? keyed.partitionKey() : eventType;
        log.info("Publishing event {} to kafka", eventType);
        try {
            kafkaTemplate.send(topic, key, event).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted when sending event: {}", e);
            throw new IllegalStateException("Bị gián đoạn khi gửi event");
        }
        catch (ExecutionException e) {
            log.error("Event published failed to kafka: {}", e);
            throw new IllegalStateException("Có lỗi xảy ra, không thể gửi event lên kafka");
        }
    }

    private String resolveTopic(Object event) {
        ExternalEventTopic annotation = event.getClass().getAnnotation(ExternalEventTopic.class);
        return annotation == null ? toKebabCase(event.getClass().getSimpleName()) : annotation.value();
    }

    private String toKebabCase(String input) {
        return input.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase(Locale.ROOT);
    }

}
