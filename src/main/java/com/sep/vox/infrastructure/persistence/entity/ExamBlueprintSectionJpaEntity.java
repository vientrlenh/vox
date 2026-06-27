package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "exam_blueprint_sections")
public class ExamBlueprintSectionJpaEntity {

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

    @Column(name = "blueprint_version_id", nullable = false, updatable = false)
    private UUID blueprintVersionId;

    @Column(name = "section_order", nullable = false)
    private int order;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "instruction", length = 512)
    private String instruction;

    @Column(name = "section_time_limits_seconds")
    private Integer sectionTimeLimitSeconds;

    @Column(name = "section_weight", nullable = false, precision = 3, scale = 2)
    private BigDecimal sectionWeight;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy; 

    protected ExamBlueprintSectionJpaEntity() {}

    public ExamBlueprintSectionJpaEntity(UUID id, UUID blueprintVersionId, int order, String title, String instruction,
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
