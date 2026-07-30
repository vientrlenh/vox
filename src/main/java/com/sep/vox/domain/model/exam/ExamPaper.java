package com.sep.vox.domain.model.exam;

import java.time.Instant;
import java.util.UUID;

public class ExamPaper {
    private UUID id;
    private UUID examId;
    private UUID blueprintVersionId;
    private String code;
    private int variant;
    private ExamPaperStatus status;
    private Integer timeDurationSeconds;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public ExamPaper() {}

    public ExamPaper(UUID id, UUID examId, UUID blueprintVersionId, String code, int variant, ExamPaperStatus status, Integer timeDurationSeconds, Instant createdAt,
            Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.examId = examId;
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

    public ExamPaper(UUID examId, UUID blueprintVersionId, String code, int variant, ExamPaperStatus status, Integer timeDurationSeconds, Instant createdAt,
            Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.examId = examId;
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

    public UUID getExamId() {
        return examId;
    }

    public void setExamId(UUID examId) {
        this.examId = examId;
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

    public ExamPaperStatus getStatus() {
        return status;
    }

    public void setStatus(ExamPaperStatus status) {
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
