package com.sep.vox.domain.model.assessment;

import java.time.Instant;
import java.util.UUID;

public class AssessmentItemAnswer {
    private UUID id;
    private UUID sessionId;
    private UUID paperItemId;
    private String audioUrl;
    private Integer durationSeconds;
    private String transcript; 
    private String terminationReason;
    private Instant submittedAt;

    protected AssessmentItemAnswer() {}

    public AssessmentItemAnswer(UUID id, UUID sessionId, UUID paperItemId, String audioUrl, Integer durationSeconds,
            String transcript, String terminationReason, Instant submittedAt) {
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
