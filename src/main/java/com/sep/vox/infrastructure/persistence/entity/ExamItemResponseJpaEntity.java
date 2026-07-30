package com.sep.vox.infrastructure.persistence.entity;

import java.time.Instant;
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

    // TEXT, not a short varchar: the value is a free-text reason from the AI's follow-up decision
    // (e.g. "The student has only provided one reason for enjoying reading and has not
    // elaborated..."), not a short status code -- length = 100 truncated nothing, it just made
    // every save fail with "value too long" whenever the reason ran past 100 characters, which is
    // the common case, not an edge case. Matches the transcript column's pattern above.
    @Column(name = "termination_reason", columnDefinition = "TEXT")
    private String terminationReason;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    protected ExamItemResponseJpaEntity() {}

    public ExamItemResponseJpaEntity(UUID id, UUID sessionId, UUID paperItemId, String audioUrl,
            Integer durationSeconds, String transcript, String terminationReason, Instant submittedAt) {
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

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

}
