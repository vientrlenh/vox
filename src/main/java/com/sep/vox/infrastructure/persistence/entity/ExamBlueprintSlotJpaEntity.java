package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "exam_blueprint_slots")
public class ExamBlueprintSlotJpaEntity {

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

    @Column(name = "section_id", updatable = false)
    private UUID sectionId;

    @Column(name = "blueprint_version_id", updatable = false)
    private UUID blueprintVersionId;

    @Column(name = "slot_order", nullable = false)
    private int order;

    @Column(name = "weight", nullable = false, precision = 3, scale = 2)
    private BigDecimal weight;

    @Column(name = "prep_time_seconds_override")
    private Integer prepTimeSecondsOverride;

    @Column(name = "response_time_seconds_override")
    private Integer responseTimeSecondsOverride;

    @Column(name = "slot_type", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_exam_blueprint_slots_type_valid", 
            constraint = "slot_type IN ('FIXED', 'SELECTION')"
        )
    })
    private String slotType;

    @Column(name = "fixed_question_id", updatable = false)
    private UUID fixedQuestionId;

    @Column(name = "selection_spec", updatable = false, columnDefinition = "TEXT")
    private String selectionSpec;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected ExamBlueprintSlotJpaEntity() {}

    public ExamBlueprintSlotJpaEntity(UUID id, UUID sectionId, UUID blueprintVersionId, int order, BigDecimal weight,
            Integer prepTimeSecondsOverride, Integer responseTimeSecondsOverride, String slotType, UUID fixedQuestionId,
            String selectionSpec, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
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

    public String getSlotType() {
        return slotType;
    }

    public void setSlotType(String slotType) {
        this.slotType = slotType;
    }

    public UUID getFixedQuestionId() {
        return fixedQuestionId;
    }

    public void setFixedQuestionId(UUID fixedQuestionId) {
        this.fixedQuestionId = fixedQuestionId;
    }

    public String getSelectionSpec() {
        return selectionSpec;
    }

    public void setSelectionSpec(String selectionSpec) {
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
