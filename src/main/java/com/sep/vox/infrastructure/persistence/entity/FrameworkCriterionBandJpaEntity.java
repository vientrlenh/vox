package com.sep.vox.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "framework_criterion_bands", indexes = {
    @Index(columnList = "framework_criterion_id, framework_result_band_id", name = "idx_framework_criterion_bands_criterion_result", unique = true),
    @Index(columnList = "framework_result_band_id", name = "idx_framework_criterion_bands_result")
})
public class FrameworkCriterionBandJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(name = "id", nullable = false, updatable = false, insertable = false, columnDefinition = "UUID DEFAULT uuidv7()")
    private UUID id;

    @Column(name = "framework_criterion_id", nullable = false, updatable = false)
    private UUID frameworkCriterionId;

    @Column(name = "framework_result_band_id", nullable = false, updatable = false)
    private UUID frameworkResultBandId;

    @Column(name = "descriptor", columnDefinition = "TEXT")
    private String descriptor;

    @Column(name = "positive_signals", columnDefinition = "TEXT")
    private String positiveSignals;

    @Column(name = "negative_signals", columnDefinition = "TEXT")
    private String negativeSignals;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected FrameworkCriterionBandJpaEntity() {}

    public FrameworkCriterionBandJpaEntity(UUID id, UUID frameworkCriterionId, UUID frameworkResultBandId,
            String descriptor, String positiveSignals, String negativeSignals, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.frameworkCriterionId = frameworkCriterionId;
        this.frameworkResultBandId = frameworkResultBandId;
        this.descriptor = descriptor;
        this.positiveSignals = positiveSignals;
        this.negativeSignals = negativeSignals;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getFrameworkCriterionId() { return frameworkCriterionId; }
    public void setFrameworkCriterionId(UUID frameworkCriterionId) { this.frameworkCriterionId = frameworkCriterionId; }
    public UUID getFrameworkResultBandId() { return frameworkResultBandId; }
    public void setFrameworkResultBandId(UUID frameworkResultBandId) { this.frameworkResultBandId = frameworkResultBandId; }
    public String getDescriptor() { return descriptor; }
    public void setDescriptor(String descriptor) { this.descriptor = descriptor; }
    public String getPositiveSignals() { return positiveSignals; }
    public void setPositiveSignals(String positiveSignals) { this.positiveSignals = positiveSignals; }
    public String getNegativeSignals() { return negativeSignals; }
    public void setNegativeSignals(String negativeSignals) { this.negativeSignals = negativeSignals; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
}
