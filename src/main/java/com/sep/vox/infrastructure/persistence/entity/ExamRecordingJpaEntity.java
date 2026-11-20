package com.sep.vox.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "exam_recordings",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_exam_recordings_session_stream_source",
        columnNames = {"exam_session_id", "stream_type", "source"}
    )
)
public class ExamRecordingJpaEntity {
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

    @Column(name = "exam_session_id", nullable = false, updatable = false)
    private UUID examSessionId;

    @Column(name = "candidate_id", nullable = false, updatable = false)
    private UUID candidateId;

    @Column(name = "stream_type", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_exam_recordings_stream_type_valid", 
            constraint = "stream_type IN ('SCREEN', 'CAMERA')"
        )
    })
    private String streamType;

    @Column(name = "bucket", nullable = false, length = 2048)
    private String bucket;

    @Column(name = "s3_key", nullable = false, length = 1024)
    private String s3Key;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_exam_recordings_status_valid", 
            constraint = "status IN ('PROCESSING', 'READY', 'PARTIAL', 'FAILED', 'ABANDONED')"
        )
    })
    private String status;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    // Nullable để schema update tương thích với recording cũ chưa có source. Event mới luôn được
    // mapper chuẩn hóa thành source cụ thể hoặc UNKNOWN.
    @Column(name = "source", updatable = false, length = 32)
    private String source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "assembled_at")
    private Instant assembledAt;

    protected ExamRecordingJpaEntity() {}

    public ExamRecordingJpaEntity(UUID id, UUID examSessionId, UUID candidateId, String streamType, String bucket, String s3Key, 
            String status, Long sizeBytes, Integer durationSeconds, String source, Instant createdAt,
            Instant assembledAt) {
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

    public String getStreamType() {
        return streamType;
    }

    public void setStreamType(String streamType) {
        this.streamType = streamType;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    
}
