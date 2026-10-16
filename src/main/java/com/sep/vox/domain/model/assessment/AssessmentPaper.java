package com.sep.vox.domain.model.assessment;

import java.time.Instant;
import java.util.UUID;

public class AssessmentPaper {
    private UUID id;
    private UUID assessmentId;
    private UUID blueprintVersionId;
    private String code;
    private int variant;
    private AssessmentPaperStatus status;
    private Integer timeDurationSeconds;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public AssessmentPaper() {}

    public AssessmentPaper(UUID id, UUID assessmentId, UUID blueprintVersionId, String code, int variant, AssessmentPaperStatus status, Integer timeDurationSeconds, Instant createdAt,
            Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.assessmentId = assessmentId;
        this.blueprintVersionId = blueprintVersionId;
        this.code = code;
        this.variant = variant;
        this.status = status;
        this.timeDurationSeconds = timeDurationSeconds;
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

    public UUID getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(UUID assessmentId) {
        this.assessmentId = assessmentId;
    }

    public UUID getBlueprintVersionId() {
        return blueprintVersionId;
    }

    public void setBlueprintVersionId(UUID blueprintVersionId) {
        this.blueprintVersionId = blueprintVersionId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getVariant() {
        return variant;
    }

    public void setVariant(int variant) {
        this.variant = variant;
    }

    public AssessmentPaperStatus getStatus() {
        return status;
    }

    public void setStatus(AssessmentPaperStatus status) {
        this.status = status;
    }

    public Integer getTimeDurationSeconds() {
        return timeDurationSeconds;
    }

    public void setTimeDurationSeconds(Integer timeDurationSeconds) {
        this.timeDurationSeconds = timeDurationSeconds;
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
