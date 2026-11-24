package com.sep.vox.domain.model.assessment;

import java.time.Instant;
import java.util.UUID;

public class AssessmentRecording {
    private UUID id;
    private UUID assessmentSessionId;
    private UUID candidateId;
    private AssessmentRequiredStreamType streamType;
    private String bucket;
    private String s3Key;
    private AssessmentRecordingAssemblyStatus status;
    private Long sizeBytes;
    private Integer durationSeconds;
    private String source;
    private Instant createdAt;
    private Instant assembledAt;

    public AssessmentRecording() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAssessmentSessionId() {
        return assessmentSessionId;
    }

    public void setAssessmentSessionId(UUID assessmentSessionId) {
        this.assessmentSessionId = assessmentSessionId;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(UUID candidateId) {
        this.candidateId = candidateId;
    }

    public AssessmentRequiredStreamType getStreamType() {
        return streamType;
    }

    public void setStreamType(AssessmentRequiredStreamType streamType) {
        this.streamType = streamType;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public AssessmentRecordingAssemblyStatus getStatus() {
        return status;
    }

    public void setStatus(AssessmentRecordingAssemblyStatus status) {
        this.status = status;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getAssembledAt() {
        return assembledAt;
    }

    public void setAssembledAt(Instant assembledAt) {
        this.assembledAt = assembledAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public static AssessmentRecording registered(
            UUID assessmentSessionId, UUID candidateId, AssessmentRequiredStreamType streamType, String source,
            AssessmentRecordingAssemblyStatus status, Instant createdAt) {
        return builder()
            .assessmentSessionId(assessmentSessionId)
            .candidateId(candidateId)
            .streamType(streamType)
            .bucket("")
            .s3Key("")
            .status(status)
            .sizeBytes(null)
            .durationSeconds(null)
            .source(source)
            .createdAt(createdAt)
            .assembledAt(null)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final AssessmentRecording target = new AssessmentRecording();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder assessmentSessionId(UUID assessmentSessionId) {
            target.assessmentSessionId = assessmentSessionId;
            return this;
        }

        public Builder candidateId(UUID candidateId) {
            target.candidateId = candidateId;
            return this;
        }

        public Builder streamType(AssessmentRequiredStreamType streamType) {
            target.streamType = streamType;
            return this;
        }

        public Builder bucket(String bucket) {
            target.bucket = bucket;
            return this;
        }

        public Builder s3Key(String s3Key) {
            target.s3Key = s3Key;
            return this;
        }

        public Builder status(AssessmentRecordingAssemblyStatus status) {
            target.status = status;
            return this;
        }

        public Builder sizeBytes(Long sizeBytes) {
            target.sizeBytes = sizeBytes;
            return this;
        }

        public Builder durationSeconds(Integer durationSeconds) {
            target.durationSeconds = durationSeconds;
            return this;
        }

        public Builder source(String source) {
            target.source = source;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            target.createdAt = createdAt;
            return this;
        }

        public Builder assembledAt(Instant assembledAt) {
            target.assembledAt = assembledAt;
            return this;
        }

        public AssessmentRecording build() {
            return target;
        }

    }
}
