package com.sep.vox.infrastructure.config;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.infrastructure.properties.OutboxTopicProperties;

@Configuration
@EnableConfigurationProperties(OutboxTopicProperties.class)
@Slf4j
public class OutboxPropertiesConfig {

    @Bean
    InitializingBean outboxTopicsValidator(OutboxTopicProperties properties) {
        return () -> {
            Map<String, String> topics = properties.topics();
            if (topics.isEmpty()) {
                throw new IllegalStateException("app.outbox.topics is not configured -- the outbox cannot publish any event");
            }

            List<String> missing = EventTypeConstant.all().stream()
                .filter(eventType -> !topics.containsKey(eventType))
                .toList();
            if (!missing.isEmpty()) {
                throw new IllegalStateException("Missing app.outbox.topics for eventType: " + missing);
            }

            topics.forEach((eventType, topic) -> log.info("Outbox routing: {} -> {}", eventType, topic));
        };
    }
}
