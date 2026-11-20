package com.sep.vox.domain.model.exam;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ExamBlueprintSection {
    private UUID id;
    private UUID blueprintVersionId;
    private int order;
    private String title;
    private String instruction;
    private Integer sectionTimeLimitSeconds;
    private BigDecimal sectionWeight;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public ExamBlueprintSection() {}

    public ExamBlueprintSection(UUID id, UUID blueprintVersionId, int order, String title, String instruction,
            Integer sectionTimeLimitSeconds, BigDecimal sectionWeight, Instant createdAt,
            Instant updatedAt, UUID createdBy, UUID updatedBy) {
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
            Integer sectionTimeLimitSeconds, BigDecimal sectionWeight, Instant createdAt,
            Instant updatedAt, UUID createdBy, UUID updatedBy) {
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
