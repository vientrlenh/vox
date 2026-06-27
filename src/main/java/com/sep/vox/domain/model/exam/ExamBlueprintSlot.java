package com.sep.vox.domain.model.exam;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.sep.vox.domain.valueobject.QuestionSelectionSpec;

public class ExamBlueprintSlot {
    private UUID id;
    private UUID sectionId;
    private UUID blueprintVersionId;
    private int order;
    private BigDecimal weight;
    private Integer prepTimeSecondsOverride; // đè default
    private Integer responseTimeSecondsOverride;
    private ExamBlueprintSlotType slotType; 
    private UUID fixedQuestionId; // set khi FIXED
    private QuestionSelectionSpec selectionSpec; // set khi SELECTION
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public ExamBlueprintSlot() {}

    public ExamBlueprintSlot(UUID id, UUID sectionId, UUID blueprintVersionId, int order, BigDecimal weight,
            Integer prepTimeSecondsOverride, Integer responseTimeSecondsOverride, ExamBlueprintSlotType slotType,
            UUID fixedQuestionId, QuestionSelectionSpec selectionSpec, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.sectionId = sectionId;
        this.blueprintVersionId = blueprintVersionId;
        this.order = order;
        this.weight = weight;
        this.prepTimeSecondsOverride = prepTimeSecondsOverride;
        this.responseTimeSecondsOverride = responseTimeSecondsOverride;
        this.slotType = slotType;
        this.fixedQuestionId = fixedQuestionId;
        this.selectionSpec = selectionSpec;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public ExamBlueprintSlot(UUID sectionId, UUID blueprintVersionId, int order, BigDecimal weight,
            Integer prepTimeSecondsOverride, Integer responseTimeSecondsOverride, ExamBlueprintSlotType slotType,
            UUID fixedQuestionId, QuestionSelectionSpec selectionSpec, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.sectionId = sectionId;
        this.blueprintVersionId = blueprintVersionId;
        this.order = order;
        this.weight = weight;
        this.prepTimeSecondsOverride = prepTimeSecondsOverride;
        this.responseTimeSecondsOverride = responseTimeSecondsOverride;
        this.slotType = slotType;
        this.fixedQuestionId = fixedQuestionId;
        this.selectionSpec = selectionSpec;
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

    public UUID getSectionId() {
        return sectionId;
    }

    public void setSectionId(UUID sectionId) {
        this.sectionId = sectionId;
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

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public Integer getPrepTimeSecondsOverride() {
        return prepTimeSecondsOverride;
    }

    public void setPrepTimeSecondsOverride(Integer prepTimeSecondsOverride) {
        this.prepTimeSecondsOverride = prepTimeSecondsOverride;
    }

    public Integer getResponseTimeSecondsOverride() {
        return responseTimeSecondsOverride;
    }

    public void setResponseTimeSecondsOverride(Integer responseTimeSecondsOverride) {
        this.responseTimeSecondsOverride = responseTimeSecondsOverride;
    }

    public ExamBlueprintSlotType getSlotType() {
        return slotType;
    }

    public void setSlotType(ExamBlueprintSlotType slotType) {
        this.slotType = slotType;
    }

    public UUID getFixedQuestionId() {
        return fixedQuestionId;
    }

    public void setFixedQuestionId(UUID fixedQuestionId) {
        this.fixedQuestionId = fixedQuestionId;
    }

    public QuestionSelectionSpec getSelectionSpec() {
        return selectionSpec;
    }

    public void setSelectionSpec(QuestionSelectionSpec selectionSpec) {
        this.selectionSpec = selectionSpec;
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
