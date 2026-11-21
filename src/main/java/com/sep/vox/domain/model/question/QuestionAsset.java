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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final QuestionAsset target = new QuestionAsset();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder questionId(UUID questionId) {
            target.questionId = questionId;
            return this;
        }

        public Builder title(String title) {
            target.title = title;
            return this;
        }

        public Builder durationSeconds(Integer durationSeconds) {
            target.durationSeconds = durationSeconds;
            return this;
        }

        public Builder altText(String altText) {
            target.altText = altText;
            return this;
        }

        public Builder type(QuestionAssetType type) {
            target.type = type;
            return this;
        }

        public Builder url(String url) {
            target.url = url;
            return this;
        }

        public Builder transcript(String transcript) {
            target.transcript = transcript;
            return this;
        }

        public Builder description(String description) {
            target.description = description;
            return this;
        }

        public Builder order(int order) {
            target.order = order;
            return this;
        }

        public QuestionAsset build() {
            return target;
        }
    }
}
