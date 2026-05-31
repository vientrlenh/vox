package com.sep.vox.domain.model.question;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.sep.vox.domain.valueobject.DifficultyLevel;
import com.sep.vox.domain.valueobject.QuestionType;

public class Question {
    private UUID id;
    private UUID topicId;
    private String questionText;
    private String audioUrl;
    private DifficultyLevel difficultyLevel;
    private QuestionType questionType;
    private int durationSeconds;
    private boolean isActive;
    private OffsetDateTime createdAt;

    public Question() {}

    public Question(UUID id, UUID topicId, String questionText, String audioUrl, DifficultyLevel difficultyLevel,
            QuestionType questionType, int durationSeconds, boolean isActive, OffsetDateTime createdAt) {
        this.id = id;
        this.topicId = topicId;
        this.questionText = questionText;
        this.audioUrl = audioUrl;
        this.difficultyLevel = difficultyLevel;
        this.questionType = questionType;
        this.durationSeconds = durationSeconds;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public Question(UUID topicId, String questionText, String audioUrl, DifficultyLevel difficultyLevel,
            QuestionType questionType, int durationSeconds, boolean isActive, OffsetDateTime createdAt) {
        this.topicId = topicId;
        this.questionText = questionText;
        this.audioUrl = audioUrl;
        this.difficultyLevel = difficultyLevel;
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

    public DifficultyLevel getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(DifficultyLevel difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
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
