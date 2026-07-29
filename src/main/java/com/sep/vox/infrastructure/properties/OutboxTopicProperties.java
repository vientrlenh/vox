package com.sep.vox.infrastructure.properties;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.outbox")
public record OutboxTopicProperties(
    Map<String, String> topics
) {
    public OutboxTopicProperties {
        topics = topics == null ? Map.of() : Map.copyOf(topics);
    }
}
