package com.sep.vox.infrastructure.persistence.entity;

import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "question_topics", indexes = {
    @Index(columnList = "bank_id", name = "idx_question_topic_bank")
})
public class QuestionTopicJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(name = "id", nullable = false, updatable = false, insertable = false, columnDefinition = "UUID DEFAULT uuidv7()")
    private UUID id;

    @Column(name = "bank_id", nullable = false)
    private UUID bankId;

    @Column(name = "topic_name", nullable = false, length = 255)
    private String topicName;

    @Column(name = "description", length = 2048)
    private String description;

    protected QuestionTopicJpaEntity() {}

    public QuestionTopicJpaEntity(UUID id, UUID bankId, String topicName, String description) {
        this.id = id;
        this.bankId = bankId;
        this.topicName = topicName;
        this.description = description;
    }

    public QuestionTopicJpaEntity(UUID bankId, String topicName, String description) {
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
