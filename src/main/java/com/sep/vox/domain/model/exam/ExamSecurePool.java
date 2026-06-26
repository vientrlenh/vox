package com.sep.vox.domain.model.exam;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ExamSecurePool {
    private UUID id;
    private UUID examId;
    private ExamSecurePoolStatus status;
    private ExamSecurePoolReleaseMode releaseMode;
    private OffsetDateTime embargoUntil; // auto thì tự release sau thời gian này
    private OffsetDateTime releasedAt;
    private UUID releasedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public ExamSecurePool() {}

    public ExamSecurePool(UUID id, UUID examId, ExamSecurePoolStatus status, ExamSecurePoolReleaseMode releaseMode,
            OffsetDateTime embargoUntil, OffsetDateTime releasedAt, UUID releasedBy, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.examId = examId;
        this.status = status;
        this.releaseMode = releaseMode;
        this.embargoUntil = embargoUntil;
        this.releasedAt = releasedAt;
        this.releasedBy = releasedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public ExamSecurePool(UUID examId, ExamSecurePoolStatus status, ExamSecurePoolReleaseMode releaseMode,
            OffsetDateTime embargoUntil, OffsetDateTime releasedAt, UUID releasedBy, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.examId = examId;
        this.status = status;
        this.releaseMode = releaseMode;
        this.embargoUntil = embargoUntil;
        this.releasedAt = releasedAt;
        this.releasedBy = releasedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getExamId() {
        return examId;
    }

    public void setExamId(UUID examId) {
        this.examId = examId;
    }

    public ExamSecurePoolStatus getStatus() {
        return status;
    }

    public void setStatus(ExamSecurePoolStatus status) {
        this.status = status;
    }

    public ExamSecurePoolReleaseMode getReleaseMode() {
        return releaseMode;
    }

    public void setReleaseMode(ExamSecurePoolReleaseMode releaseMode) {
        this.releaseMode = releaseMode;
    }

    public OffsetDateTime getEmbargoUntil() {
        return embargoUntil;
    }

    public void setEmbargoUntil(OffsetDateTime embargoUntil) {
        this.embargoUntil = embargoUntil;
    }

    public OffsetDateTime getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(OffsetDateTime releasedAt) {
        this.releasedAt = releasedAt;
    }

    public UUID getReleasedBy() {
        return releasedBy;
    }

    public void setReleasedBy(UUID releasedBy) {
        this.releasedBy = releasedBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    
}
