package com.sep.vox.infrastructure.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.external-events.kafka")
public class ExternalEventKafkaProperties {

    private String bootstrapServers;
    private String clientId;
    private String acks = "all";
    private String topicPrefix;
    private final Map<String, String> topics = new LinkedHashMap<>();
    private final Map<String, ConsumerGroupConfig> consumerGroups = new LinkedHashMap<>();

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getAcks() {
        return acks;
    }

    public void setAcks(String acks) {
        this.acks = acks;
    }

    public String getTopicPrefix() {
        return topicPrefix;
    }

    public void setTopicPrefix(String topicPrefix) {
        this.topicPrefix = topicPrefix;
    }

    public Map<String, String> getTopics() {
        return topics;
    }

    public Map<String, ConsumerGroupConfig> getConsumerGroups() {
        return consumerGroups;
    }

    public static class ConsumerGroupConfig {

        private String groupId;
        private String topics;

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getTopics() {
            return topics;
        }

        public void setTopics(String topics) {
            this.topics = topics;
        }
    }
}
