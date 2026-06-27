package com.sep.vox.domain.model.exam;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ExamBlueprintSection {
    private UUID id;
    private UUID blueprintVersionId;
    private int order;
    private String title;
    private String instruction;
    private Integer sectionTimeLimitSeconds;
    private BigDecimal sectionWeight;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public ExamBlueprintSection() {}

    public ExamBlueprintSection(UUID id, UUID blueprintVersionId, int order, String title, String instruction,
            Integer sectionTimeLimitSeconds, BigDecimal sectionWeight, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.blueprintVersionId = blueprintVersionId;
        this.order = order;
        this.title = title;
        this.instruction = instruction;
        this.sectionTimeLimitSeconds = sectionTimeLimitSeconds;
        this.sectionWeight = sectionWeight;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public ExamBlueprintSection(UUID blueprintVersionId, int order, String title, String instruction,
            Integer sectionTimeLimitSeconds, BigDecimal sectionWeight, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.blueprintVersionId = blueprintVersionId;
        this.order = order;
        this.title = title;
        this.instruction = instruction;
        this.sectionTimeLimitSeconds = sectionTimeLimitSeconds;
        this.sectionWeight = sectionWeight;
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

    public UUID getBlueprintVersionId() {
        return blueprintVersionId;
    }

    public void setBlueprintVersionId(UUID blueprintVersionId) {
        this.blueprintVersionId = blueprintVersionId;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public Integer getSectionTimeLimitSeconds() {
        return sectionTimeLimitSeconds;
    }

    public void setSectionTimeLimitSeconds(Integer sectionTimeLimitSeconds) {
        this.sectionTimeLimitSeconds = sectionTimeLimitSeconds;
    }

    public BigDecimal getSectionWeight() {
        return sectionWeight;
    }

    public void setSectionWeight(BigDecimal sectionWeight) {
        this.sectionWeight = sectionWeight;
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
