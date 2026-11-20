package com.sep.vox.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.infrastructure.exception.InfrastructureException;
import com.sep.vox.infrastructure.properties.OutboxTopicProperties;

@Configuration
@EnableConfigurationProperties(OutboxTopicProperties.class)
public class OutboxPropertiesConfig {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxPropertiesConfig.class);

    @Bean
    InitializingBean outboxTopicsValidator(OutboxTopicProperties properties) {
        return () -> {
            var topics = properties.topics();
            if (topics.isEmpty()) {
                throw new InfrastructureException("app.outbox.topics chưa được cấu hình -- outbox không publish được event nào");
            }

            var missing = EventTypeConstant.all().stream()
                .filter(eventType -> !topics.containsKey(eventType))
                .toList();
            if (!missing.isEmpty()) {
                throw new InfrastructureException("Thiếu app.outbox.topics cho eventType: " + missing);
            }

            topics.forEach((eventType, topic) -> LOGGER.info("Outbox routing: {} -> {}", eventType, topic));
        };
    }
}
