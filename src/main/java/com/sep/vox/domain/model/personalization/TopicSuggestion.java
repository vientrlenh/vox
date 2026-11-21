package com.sep.vox.domain.model.personalization;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TopicSuggestion {

    private UUID id;
    private UUID studentId;
    private String suggestedTopicName;
    private String keyword;
    private String interestDimension;
    private String curriculumGroup;
    private BigDecimal confidence;
    private String reasonText;
    private String evidenceJson;
    private String status;
    private Instant createdAt;
    private Instant respondedAt;

    public TopicSuggestion() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
    }

    public String getSuggestedTopicName() {
        return suggestedTopicName;
    }

    public void setSuggestedTopicName(String suggestedTopicName) {
        this.suggestedTopicName = suggestedTopicName;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getInterestDimension() {
        return interestDimension;
    }

    public void setInterestDimension(String interestDimension) {
        this.interestDimension = interestDimension;
    }

    public String getCurriculumGroup() {
        return curriculumGroup;
    }

    public void setCurriculumGroup(String curriculumGroup) {
        this.curriculumGroup = curriculumGroup;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public String getReasonText() {
        return reasonText;
    }

    public void setReasonText(String reasonText) {
        this.reasonText = reasonText;
    }

    public String getEvidenceJson() {
        return evidenceJson;
    }

    public void setEvidenceJson(String evidenceJson) {
        this.evidenceJson = evidenceJson;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Instant respondedAt) {
        this.respondedAt = respondedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final TopicSuggestion target = new TopicSuggestion();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder studentId(UUID studentId) {
            target.studentId = studentId;
            return this;
        }

        public Builder suggestedTopicName(String suggestedTopicName) {
            target.suggestedTopicName = suggestedTopicName;
            return this;
        }

        public Builder keyword(String keyword) {
            target.keyword = keyword;
            return this;
        }

        public Builder interestDimension(String interestDimension) {
            target.interestDimension = interestDimension;
            return this;
        }

        public Builder curriculumGroup(String curriculumGroup) {
            target.curriculumGroup = curriculumGroup;
            return this;
        }

        public Builder confidence(BigDecimal confidence) {
            target.confidence = confidence;
            return this;
        }

        public Builder reasonText(String reasonText) {
            target.reasonText = reasonText;
            return this;
        }

        public Builder evidenceJson(String evidenceJson) {
            target.evidenceJson = evidenceJson;
            return this;
        }

        public Builder status(String status) {
            target.status = status;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            target.createdAt = createdAt;
            return this;
        }

        public Builder respondedAt(Instant respondedAt) {
            target.respondedAt = respondedAt;
            return this;
        }

        public TopicSuggestion build() {
            return target;
        }

    }
}
