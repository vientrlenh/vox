package com.sep.vox.domain.model.assessment;

import java.time.Instant;
import java.util.UUID;

public class AssessmentSecurePool {
    private UUID id;
    private UUID examId;
    private AssessmentSecurePoolStatus status;
    private AssessmentSecurePoolReleaseMode releaseMode;
    private Instant embargoUntil;
    private Instant releasedAt;
    private UUID releasedBy;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    protected AssessmentSecurePool() {}

    public AssessmentSecurePool(UUID id, UUID examId, AssessmentSecurePoolStatus status, AssessmentSecurePoolReleaseMode releaseMode,
            Instant embargoUntil, Instant releasedAt, UUID releasedBy, Instant createdAt,
            Instant updatedAt, UUID createdBy, UUID updatedBy) {
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

    public AssessmentSecurePoolStatus getStatus() {
        return status;
    }

    public void setStatus(AssessmentSecurePoolStatus status) {
        this.status = status;
    }

    public AssessmentSecurePoolReleaseMode getReleaseMode() {
        return releaseMode;
    }

    public void setReleaseMode(AssessmentSecurePoolReleaseMode releaseMode) {
        this.releaseMode = releaseMode;
    }

    public Instant getEmbargoUntil() {
        return embargoUntil;
    }

    public void setEmbargoUntil(Instant embargoUntil) {
        this.embargoUntil = embargoUntil;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(Instant releasedAt) {
        this.releasedAt = releasedAt;
    }

    public UUID getReleasedBy() {
        return releasedBy;
    }

    public void setReleasedBy(UUID releasedBy) {
        this.releasedBy = releasedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
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
