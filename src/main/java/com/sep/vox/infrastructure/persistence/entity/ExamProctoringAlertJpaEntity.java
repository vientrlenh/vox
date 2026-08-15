package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "exam_proctoring_alerts",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_exam_proctoring_alert_event_id",
        columnNames = {"event_id"}
    ),
    indexes = {
        @Index(name = "idx_exam_proctoring_alert_session", columnList = "exam_session_id, captured_at"),
        @Index(name = "idx_exam_proctoring_alert_candidate", columnList = "candidate_id, captured_at")
    }
)
public class ExamProctoringAlertJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id",
        nullable = false,
        updatable = false,
        insertable = false,
        columnDefinition = "UUID DEFAULT uuidv7()"
    )
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false, length = 64)
    private String eventId;

    @Column(name = "exam_session_id", nullable = false, updatable = false)
    private UUID examSessionId;

    @Column(name = "candidate_id", updatable = false)
    private UUID candidateId;

    @Column(name = "stream_id", updatable = false, length = 64)
    private String streamId;

    @Column(name = "stream_type", updatable = false, length = 20)
    private String streamType;

    @Column(name = "alert_type", nullable = false, updatable = false, length = 64)
    private String alertType;

    @Column(name = "level", updatable = false, length = 16)
    private String level;

    @Column(name = "source", updatable = false, length = 32)
    private String source;

    @Column(name = "detail", updatable = false, length = 1024)
    private String detail;

    @Column(name = "confidence", updatable = false, precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "sequence_no", updatable = false)
    private Long sequenceNo;

    @Column(name = "captured_at", nullable = false, updatable = false)
    private Instant capturedAt;

    @Column(name = "raised_at", nullable = false, updatable = false)
    private Instant raisedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public UUID getExamSessionId() {
        return examSessionId;
    }

    public void setExamSessionId(UUID examSessionId) {
        this.examSessionId = examSessionId;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(UUID candidateId) {
        this.candidateId = candidateId;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public String getStreamType() {
        return streamType;
    }

    public void setStreamType(String streamType) {
        this.streamType = streamType;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public Long getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(Long sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
    }

    public Instant getRaisedAt() {
        return raisedAt;
    }

    public void setRaisedAt(Instant raisedAt) {
        this.raisedAt = raisedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
