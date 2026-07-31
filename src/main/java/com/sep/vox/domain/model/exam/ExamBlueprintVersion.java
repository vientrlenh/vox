package com.sep.vox.domain.model.exam;

import java.time.Instant;
import java.util.UUID;

public class ExamBlueprintVersion {
    private UUID id;
    private UUID blueprintId;
    private int version;
    private String code;
    private String description;
    private ExamBlueprintVersionStatus status;
    private Integer totalTimeLimitSeconds;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    
    public ExamBlueprintVersion() {}

    public ExamBlueprintVersion(UUID id, UUID blueprintId, int version, String code, String description,
            ExamBlueprintVersionStatus status, Integer totalTimeLimitSeconds, Instant effectiveFrom,
            Instant effectiveTo, Instant createdAt, Instant updatedAt, UUID createdBy,
            UUID updatedBy) {
        this.id = id;
        this.blueprintId = blueprintId;
        this.version = version;
        this.code = code;
        this.description = description;
        this.status = status;
        this.totalTimeLimitSeconds = totalTimeLimitSeconds;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public ExamBlueprintVersion(UUID blueprintId, int version, String code, String description,
            ExamBlueprintVersionStatus status, Integer totalTimeLimitSeconds, Instant effectiveFrom,
            Instant effectiveTo, Instant createdAt, Instant updatedAt, UUID createdBy,
            UUID updatedBy) {
        this.blueprintId = blueprintId;
        this.version = version;
        this.code = code;
        this.description = description;
        this.status = status;
        this.totalTimeLimitSeconds = totalTimeLimitSeconds;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
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

    public UUID getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(UUID blueprintId) {
        this.blueprintId = blueprintId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ExamBlueprintVersionStatus getStatus() {
        return status;
    }

    public void setStatus(ExamBlueprintVersionStatus status) {
        this.status = status;
    }

    public Integer getTotalTimeLimitSeconds() {
        return totalTimeLimitSeconds;
    }

    public void setTotalTimeLimitSeconds(Integer totalTimeLimitSeconds) {
        this.totalTimeLimitSeconds = totalTimeLimitSeconds;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(Instant effectiveTo) {
        this.effectiveTo = effectiveTo;
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
