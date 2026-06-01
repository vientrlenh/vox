package com.sep.vox.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "questions", indexes = {
    @Index(columnList = "topic_id", name = "idx_question_topic"),
    @Index(columnList = "question_type", name = "idx_question_type"),
    @Index(columnList = "standard_level_id", name = "idx_question_standard_level")
})
public class QuestionJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(name = "id", nullable = false, updatable = false, insertable = false, columnDefinition = "UUID DEFAULT uuidv7()")
    private UUID id;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "audio_url", length = 512)
    private String audioUrl;

    @Column(name = "standard_level_id", nullable = false)
    private UUID standardLevelId;

    @Column(name = "question_type", nullable = false, length = 50)
    private String questionType;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected QuestionJpaEntity() {}

    public QuestionJpaEntity(UUID id, UUID topicId, String questionText, String audioUrl,
            UUID standardLevelId, String questionType, int durationSeconds, boolean isActive,
            OffsetDateTime createdAt) {
        this.id = id;
        this.topicId = topicId;
        this.questionText = questionText;
        this.audioUrl = audioUrl;
        this.standardLevelId = standardLevelId;
        this.questionType = questionType;
        this.durationSeconds = durationSeconds;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public QuestionJpaEntity(UUID topicId, String questionText, String audioUrl,
            UUID standardLevelId, String questionType, int durationSeconds, boolean isActive,
            OffsetDateTime createdAt) {
        this.topicId = topicId;
        this.questionText = questionText;
        this.audioUrl = audioUrl;
        this.standardLevelId = standardLevelId;
        this.questionType = questionType;
        this.durationSeconds = durationSeconds;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTopicId() {
        return topicId;
    }

    public void setTopicId(UUID topicId) {
        this.topicId = topicId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public UUID getStandardLevelId() {
        return standardLevelId;
    }

    public void setStandardLevelId(UUID standardLevelId) {
        this.standardLevelId = standardLevelId;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
