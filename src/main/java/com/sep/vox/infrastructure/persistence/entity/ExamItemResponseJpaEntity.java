package com.sep.vox.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "exam_item_responses")
public class ExamItemResponseJpaEntity {
    
    @Id
    @Column(
        name = "id", 
        nullable = false, 
        updatable = false
    )
    private UUID id;

    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId; 

    @Column(name = "paper_item_id")
    private UUID paperItemId;

    @Column(name = "audio_url", length = 4096)
    private String audioUrl; 

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "transcript", columnDefinition = "TEXT")
    private String transcript;

    @Column(name = "termination_reason", length = 100)
    private String terminationReason;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private OffsetDateTime submittedAt;

    protected ExamItemResponseJpaEntity() {}

    public ExamItemResponseJpaEntity(UUID id, UUID sessionId, UUID paperItemId, String audioUrl,
            Integer durationSeconds, String transcript, String terminationReason, OffsetDateTime submittedAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.paperItemId = paperItemId;
        this.audioUrl = audioUrl;
        this.durationSeconds = durationSeconds;
        this.transcript = transcript;
        this.terminationReason = terminationReason;
        this.submittedAt = submittedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public UUID getPaperItemId() {
        return paperItemId;
    }

    public void setPaperItemId(UUID paperItemId) {
        this.paperItemId = paperItemId;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public String getTerminationReason() {
        return terminationReason;
    }

    public void setTerminationReason(String terminationReason) {
        this.terminationReason = terminationReason;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(OffsetDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

}
