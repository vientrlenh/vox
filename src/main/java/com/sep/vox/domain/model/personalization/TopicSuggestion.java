package com.sep.vox.domain.model.personalization;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
    private OffsetDateTime createdAt;
    private OffsetDateTime respondedAt;

    public TopicSuggestion() {
    }

    public TopicSuggestion(
            UUID id,
            UUID studentId,
            String suggestedTopicName,
            String keyword,
            String interestDimension,
            String curriculumGroup,
            BigDecimal confidence,
            String reasonText,
            String evidenceJson,
            String status,
            OffsetDateTime createdAt,
            OffsetDateTime respondedAt) {
        this.id = id;
        this.studentId = studentId;
        this.suggestedTopicName = suggestedTopicName;
        this.keyword = keyword;
        this.interestDimension = interestDimension;
        this.curriculumGroup = curriculumGroup;
        this.confidence = confidence;
        this.reasonText = reasonText;
        this.evidenceJson = evidenceJson;
        this.status = status;
        this.createdAt = createdAt;
        this.respondedAt = respondedAt;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(OffsetDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }
}
