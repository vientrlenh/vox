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

    protected AssessmentRecording() {}

    public AssessmentRecording(UUID id, UUID assessmentSessionId, UUID candidateId, AssessmentRequiredStreamType streamType,
            String bucket, String s3Key, AssessmentRecordingAssemblyStatus status, Long sizeBytes, Integer durationSeconds, String source,
            Instant createdAt, Instant assembledAt) {
        this.id = id;
        this.assessmentSessionId = assessmentSessionId;
        this.candidateId = candidateId;
        this.streamType = streamType;
        this.bucket = bucket;
        this.s3Key = s3Key;
        this.status = status;
        this.sizeBytes = sizeBytes;
        this.durationSeconds = durationSeconds;
        this.source = source;
        this.createdAt = createdAt;
        this.assembledAt = assembledAt;
    }

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

    
}
