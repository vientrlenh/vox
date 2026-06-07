package com.sep.vox.infrastructure.config;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;

import com.fasterxml.jackson.databind.JsonNode;
import com.sep.vox.infrastructure.event.listener.KafkaExternalEventListener;
import com.sep.vox.infrastructure.event.publisher.ExternalEventKafkaValueDeserializer;
import com.sep.vox.infrastructure.event.publisher.ExternalEventKafkaValueSerializer;

@Configuration
@EnableConfigurationProperties(ExternalEventKafkaProperties.class)
@ConditionalOnProperty(prefix = "app.external-events", name = "provider", havingValue = "kafka")
public class KafkaExternalEventConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaExternalEventConfig.class);

    private Map<String, KafkaMessageListenerContainer<String, JsonNode>> listenerContainers;

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

    @Bean
    public Map<String, KafkaMessageListenerContainer<String, JsonNode>> externalEventKafkaListenerContainers(
        ExternalEventKafkaProperties properties,
        KafkaExternalEventListener listener
    ) {
        Map<String, KafkaMessageListenerContainer<String, JsonNode>> containers = new LinkedHashMap<>();

        for (var entry : properties.getConsumerGroups().entrySet()) {
            String name = entry.getKey();
            ExternalEventKafkaProperties.ConsumerGroupConfig groupConfig = entry.getValue();

            if (groupConfig.getTopics() == null || groupConfig.getTopics().isEmpty()) {
                continue;
            }

            Map<String, Object> configs = new HashMap<>();
            configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
            configs.put(ConsumerConfig.GROUP_ID_CONFIG, groupConfig.getGroupId());
            configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ExternalEventKafkaValueDeserializer.class);
            configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, groupConfig.getAutoOffsetReset());

            ConsumerFactory<String, JsonNode> consumerFactory = new DefaultKafkaConsumerFactory<>(configs);

            String[] topics = groupConfig.getTopics().toArray(String[]::new);
            ContainerProperties containerProps = new ContainerProperties(topics);
            containerProps.setMessageListener(listener);

            KafkaMessageListenerContainer<String, JsonNode> container =
                new KafkaMessageListenerContainer<>(consumerFactory, containerProps);
            container.setBeanName("externalEventListener-" + name);
            containers.put(name, container);
        }

        this.listenerContainers = containers;
        return containers;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void startListenerContainers() {
        if (listenerContainers != null) {
            for (var entry : listenerContainers.entrySet()) {
                log.info("Starting Kafka listener container: {}", entry.getKey());
                entry.getValue().start();
            }
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
