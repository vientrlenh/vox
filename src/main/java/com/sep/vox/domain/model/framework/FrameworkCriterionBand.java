package com.sep.vox.domain.model.framework;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;

public class FrameworkCriterionBand {
    private UUID id;
    private UUID frameworkCriterionId;
    private UUID frameworkResultBandId;
    private String descriptor;
    private FrameworkCriterionSignals positiveSignals;
    private FrameworkCriterionSignals negativeSignals;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public FrameworkCriterionBand() {}

    public FrameworkCriterionBand(UUID id, UUID frameworkCriterionId, UUID frameworkResultBandId, String descriptor,
            FrameworkCriterionSignals positiveSignals, FrameworkCriterionSignals negativeSignals, Instant createdAt, Instant updatedAt,
            UUID createdBy, UUID updatedBy) {
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

    public FrameworkCriterionBand(UUID frameworkCriterionId, UUID frameworkResultBandId, String descriptor,
            FrameworkCriterionSignals positiveSignals, FrameworkCriterionSignals negativeSignals, Instant createdAt, Instant updatedAt,
            UUID createdBy, UUID updatedBy) {
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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getFrameworkCriterionId() {
        return frameworkCriterionId;
    }

    public void setFrameworkCriterionId(UUID frameworkCriterionId) {
        this.frameworkCriterionId = frameworkCriterionId;
    }

    public UUID getFrameworkResultBandId() {
        return frameworkResultBandId;
    }

    public void setFrameworkResultBandId(UUID frameworkResultBandId) {
        this.frameworkResultBandId = frameworkResultBandId;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    public FrameworkCriterionSignals getPositiveSignals() {
        return positiveSignals;
    }

    public void setPositiveSignals(FrameworkCriterionSignals positiveSignals) {
        this.positiveSignals = positiveSignals;
    }

    public FrameworkCriterionSignals getNegativeSignals() {
        return negativeSignals;
    }

    public void setNegativeSignals(FrameworkCriterionSignals negativeSignals) {
        this.negativeSignals = negativeSignals;
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
