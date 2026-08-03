package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "topic_suggestion",
    indexes = {
        @Index(name = "idx_topic_suggestion_student_status", columnList = "student_id, status"),
        @Index(name = "idx_topic_suggestion_student_name", columnList = "student_id, suggested_topic_name")
    }
)
public class TopicSuggestionJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;
    @Column(name = "suggested_topic_name", nullable = false, length = 200, updatable = false)
    private String suggestedTopicName;
    @Column(name = "keyword", length = 200, updatable = false)
    private String keyword;
    @Column(name = "interest_dimension", nullable = false, length = 32, updatable = false)
    private String interestDimension;
    @Column(name = "curriculum_group", length = 24, updatable = false)
    private String curriculumGroup;
    @Column(name = "confidence", nullable = false, precision = 4, scale = 3, updatable = false)
    private BigDecimal confidence;
    @Column(name = "reason_text", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String reasonText;
    @Column(name = "evidence_json", columnDefinition = "TEXT", updatable = false)
    private String evidenceJson;
    @Column(name = "status", nullable = false, length = 16)
    private String status;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "responded_at")
    private Instant respondedAt;

    protected TopicSuggestionJpaEntity() {
    }

    public TopicSuggestionJpaEntity(
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
            Instant createdAt) {
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
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public String getSuggestedTopicName() {
        return suggestedTopicName;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getInterestDimension() {
        return interestDimension;
    }

    public String getCurriculumGroup() {
        return curriculumGroup;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public String getReasonText() {
        return reasonText;
    }

    public String getEvidenceJson() {
        return evidenceJson;
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

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Instant respondedAt) {
        this.respondedAt = respondedAt;
    }
}
