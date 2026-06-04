package com.sep.vox.domain.model.framework;

import java.time.OffsetDateTime;
import java.util.UUID;

public class FrameworkCriterionBand {
    private UUID id;
    private UUID frameworkCriterionId;
    private UUID frameworkResultBandId;
    private String descriptor;
    private String positiveSignals;
    private String negativeSignals;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public FrameworkCriterionBand() {}

    public FrameworkCriterionBand(UUID id, UUID frameworkCriterionId, UUID frameworkResultBandId, String descriptor,
            String positiveSignals, String negativeSignals, OffsetDateTime createdAt, OffsetDateTime updatedAt,
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
            String positiveSignals, String negativeSignals, OffsetDateTime createdAt, OffsetDateTime updatedAt,
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

    public String getPositiveSignals() {
        return positiveSignals;
    }

    public void setPositiveSignals(String positiveSignals) {
        this.positiveSignals = positiveSignals;
    }

    public String getNegativeSignals() {
        return negativeSignals;
    }

    public void setNegativeSignals(String negativeSignals) {
        this.negativeSignals = negativeSignals;
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
