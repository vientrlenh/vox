package com.sep.vox.domain.model.assessment;

import java.time.Instant;
import java.util.UUID;

public class AssessmentItemResponse {
    private UUID id;
    private UUID sessionId;
    private UUID paperItemId;
    private String audioUrl;
    private Integer durationSeconds;
    private String transcript; 
    private String terminationReason;
    private Instant submittedAt;

    public AssessmentItemResponse() {}

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

    public static AssessmentItemResponse answered(UUID id, UUID sessionId, UUID paperItemId, String audioUrl,
                                                  Integer durationSeconds, String transcript, Instant submittedAt) {
        return builder()
                .id(id)
                .sessionId(sessionId)
                .paperItemId(paperItemId)
                .audioUrl(audioUrl)
                .durationSeconds(durationSeconds)
                .transcript(transcript)
                .submittedAt(submittedAt)
                .build();
    }

    public static AssessmentItemResponse withoutRecording(UUID sessionId, UUID paperItemId,
                                                          String terminationReason, Instant submittedAt) {
        return builder()
                .id(UUID.randomUUID())
                .sessionId(sessionId)
                .paperItemId(paperItemId)
                .durationSeconds(0)
                .terminationReason(terminationReason)
                .submittedAt(submittedAt)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final AssessmentItemResponse target = new AssessmentItemResponse();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder sessionId(UUID sessionId) {
            target.sessionId = sessionId;
            return this;
        }

        public Builder paperItemId(UUID paperItemId) {
            target.paperItemId = paperItemId;
            return this;
        }

        public Builder audioUrl(String audioUrl) {
            target.audioUrl = audioUrl;
            return this;
        }

        public Builder durationSeconds(Integer durationSeconds) {
            target.durationSeconds = durationSeconds;
            return this;
        }

        public Builder transcript(String transcript) {
            target.transcript = transcript;
            return this;
        }

        public Builder terminationReason(String terminationReason) {
            target.terminationReason = terminationReason;
            return this;
        }

        public Builder submittedAt(Instant submittedAt) {
            target.submittedAt = submittedAt;
            return this;
        }

        public AssessmentItemResponse build() {
            return target;
        }

    }
}
