package com.sep.vox.domain.model.exam;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ExamRecording {
    private UUID id;
    private UUID examSessionId;
    private UUID candidateId;
    private ExamRequiredStreamType streamType;
    private String bucket;
    private String s3Key;
    private ExamRecordingAssemblyStatus status;
    private Long sizeBytes;
    private Integer durationSeconds;
    private String source;
    private OffsetDateTime createdAt;
    private OffsetDateTime assembledAt;

    public ExamRecording() {}

    public ExamRecording(UUID id, UUID examSessionId, UUID candidateId, ExamRequiredStreamType streamType,
            String bucket, String s3Key, ExamRecordingAssemblyStatus status, Long sizeBytes, Integer durationSeconds, String source, 
            OffsetDateTime createdAt, OffsetDateTime assembledAt) {
        this.id = id;
        this.examSessionId = examSessionId;
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

    public ExamRecording(UUID examSessionId, UUID candidateId, ExamRequiredStreamType streamType, String bucket,
            String s3Key, ExamRecordingAssemblyStatus status, Long sizeBytes, Integer durationSeconds, String source, 
            OffsetDateTime createdAt, OffsetDateTime assembledAt) {
        this.examSessionId = examSessionId;
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

    public ExamRequiredStreamType getStreamType() {
        return streamType;
    }

    public void setStreamType(ExamRequiredStreamType streamType) {
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

    public ExamRecordingAssemblyStatus getStatus() {
        return status;
    }

    public void setStatus(ExamRecordingAssemblyStatus status) {
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getAssembledAt() {
        return assembledAt;
    }

    public void setAssembledAt(OffsetDateTime assembledAt) {
        this.assembledAt = assembledAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    
}
