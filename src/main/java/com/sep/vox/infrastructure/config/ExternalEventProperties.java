package com.sep.vox.infrastructure.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
@ConfigurationProperties(prefix = "app.external-events")
public class ExternalEventProperties {

    private String topicPrefix;
    private final Map<String, ConsumerGroupConfig> consumerGroups = new LinkedHashMap<>();

    public String getTopicPrefix() {
        return topicPrefix;
    }

    public void setTopicPrefix(String topicPrefix) {
        this.topicPrefix = topicPrefix;
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
