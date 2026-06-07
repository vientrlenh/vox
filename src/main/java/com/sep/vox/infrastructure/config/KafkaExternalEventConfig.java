package com.sep.vox.infrastructure.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import com.sep.vox.infrastructure.event.publisher.ExternalEventKafkaValueSerializer;

@Configuration
@EnableConfigurationProperties(ExternalEventKafkaProperties.class)
@ConditionalOnProperty(prefix = "spring.external-events", name = "provider", havingValue = "kafka")
public class KafkaExternalEventConfig {

    @Bean
    public ProducerFactory<String, Object> externalEventProducerFactory(ExternalEventKafkaProperties properties) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ExternalEventKafkaValueSerializer.class);
        configs.put(ProducerConfig.ACKS_CONFIG, properties.getAcks());

        if (hasText(properties.getClientId())) {
            configs.put(ProducerConfig.CLIENT_ID_CONFIG, properties.getClientId());
        }

        return new DefaultKafkaProducerFactory<>(configs);
    }

    @Bean
    public KafkaTemplate<String, Object> externalEventKafkaTemplate(
        ProducerFactory<String, Object> externalEventProducerFactory
    ) {
        return new KafkaTemplate<>(externalEventProducerFactory);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
