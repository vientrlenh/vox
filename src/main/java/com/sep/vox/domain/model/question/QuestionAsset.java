package com.sep.vox.domain.model.question;

import java.util.UUID;

public class QuestionAsset {
    private UUID id;
    private UUID questionId;
    private String title;
    private Integer durationSeconds;
    private String altText;
    private QuestionAssetType type;
    private String url;
    private String transcript;
    private String description;
    private int order;

    public QuestionAsset() {}

    public QuestionAsset(UUID id, UUID questionId, String title, Integer durationSeconds, String altText, QuestionAssetType type, String url, String transcript,
            String description, int order) {
        this.id = id;
        this.questionId = questionId;
        this.title = title;
        this.durationSeconds = durationSeconds;
        this.altText = altText;
        this.type = type;
        this.url = url;
        this.transcript = transcript;
        this.description = description;
        this.order = order;
    }

    public QuestionAsset(UUID questionId, String title, Integer durationSeconds, String altText, QuestionAssetType type, String url, String transcript, String description,
            int order) {
        this.questionId = questionId;
        this.title = title;
        this.durationSeconds = durationSeconds;
        this.altText = altText;
        this.type = type;
        this.url = url;
        this.transcript = transcript;
        this.description = description;
        this.order = order;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public void setQuestionId(UUID questionId) {
        this.questionId = questionId;
    }

    public QuestionAssetType getType() {
        return type;
    }

    public void setType(QuestionAssetType type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    
}
