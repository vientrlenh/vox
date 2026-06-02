package com.sep.vox.domain.model.questiontopic;


import java.util.UUID;

public class QuestionTopic {
    private UUID id;
    private UUID bankId;
    private String topicName;
    private String description;

    public QuestionTopic() {}

    public QuestionTopic(UUID id, UUID bankId, String topicName, String description) {
        this.id = id;
        this.bankId = bankId;
        this.topicName = topicName;
        this.description = description;
    }

    public QuestionTopic(UUID bankId, String topicName, String description) {
        this.bankId = bankId;
        this.topicName = topicName;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBankId() {
        return bankId;
    }

    public void setBankId(UUID bankId) {
        this.bankId = bankId;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
